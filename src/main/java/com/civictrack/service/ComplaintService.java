package com.civictrack.service;

import com.civictrack.dto.ComplaintDtos.*;
import com.civictrack.entity.*;
import com.civictrack.repository.*;
import com.civictrack.job.*;
import org.jobrunr.scheduling.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

@Service
public class ComplaintService {
    private final ComplaintRepository complaints;
    private final DepartmentRepository departments;
    private final AssignmentRepository assignments;
    private final HistoryRepository history;
    private final NotificationRepository notifications;
    private final JobScheduler scheduler;
    private final AiClassificationJob aiJob;

    public ComplaintService(ComplaintRepository c, DepartmentRepository d, AssignmentRepository a, HistoryRepository h,
            NotificationRepository n, JobScheduler s, AiClassificationJob j) {
        complaints = c;
        departments = d;
        assignments = a;
        history = h;
        notifications = n;
        scheduler = s;
        aiJob = j;
    }

    @Transactional
    public Complaint create(User citizen, Create input) {
        Complaint c = new Complaint();
        c.citizen = citizen;
        c.title = input.title();
        c.description = input.description();
        c.category = input.category().trim().toUpperCase();
        c.location = input.location();
        c.ward = input.ward();
        c.priority = input.priority() == null ? Priority.MEDIUM : input.priority();
        c.slaDeadline = Instant.now().plus(Duration.ofHours(slaHours(c.priority)));
        c.aiStatus = "QUEUED";
        autoAssign(c);
        complaints.save(c);
        record(c, null, c.status, "Complaint submitted", citizen);
        scheduler.enqueue(() -> aiJob.classify(c.id));
        return c;
    }

    private int slaHours(Priority p) {
        return switch (p) {
            case URGENT -> 24;
            case HIGH -> 48;
            case MEDIUM -> 72;
            case LOW -> 120;
        };
    }

    private void autoAssign(Complaint c) {
        var matches = departments.findByActiveTrue().stream().filter(d -> Arrays
                .stream(d.categories.toUpperCase().split(",")).map(String::trim).anyMatch(c.category::equals)).toList();
        if (matches.size() != 1)
            return;
        Department d = matches.getFirst();
        var active = assignments.findByDepartmentIdAndActiveTrue(d.id).stream()
                .filter(a -> a.ward == null || a.ward.isBlank() || a.ward.equalsIgnoreCase(c.ward)).toList();
        if (active.size() == 1) {
            c.department = d;
            c.assignedOfficial = active.getFirst().official;
            c.assignmentStatus = "AUTO_ASSIGNED";
            c.status = ComplaintStatus.ASSIGNED;
            notify(c.assignedOfficial, "New complaint assigned: " + c.title);
        }
    }

    @Transactional
    public Complaint assign(UUID id, Assign input, User actor) {
        Complaint c = get(id);
        Department d = departments.findById(input.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        c.department = d;
        c.assignedOfficial = input.officialId() == null ? null
                : assignments.findByDepartmentIdAndActiveTrue(d.id).stream().map(a -> a.official)
                        .filter(u -> u.id.equals(input.officialId())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Official is not assigned to this department"));
        c.assignmentStatus = "MANUALLY_ASSIGNED";
        transitionInternal(c, ComplaintStatus.ASSIGNED, "Assigned by admin", actor);
        if (c.assignedOfficial != null)
            notify(c.assignedOfficial, "A complaint was assigned to you: " + c.title);
        return c;
    }

    @Transactional
    public Complaint transition(UUID id, Transition input, User actor) {
        Complaint c = get(id);
        if (actor.role == Role.CITIZEN && !c.citizen.id.equals(actor.id))
            throw new SecurityException("Not your complaint");
        if (actor.role == Role.OFFICIAL && (c.department == null || !assignments.findByOfficialIdAndActiveTrue(actor.id)
                .stream().anyMatch(a -> a.department.id.equals(c.department.id))))
            throw new SecurityException("Not assigned to this department");
        if (actor.role == Role.CITIZEN && input.status() != ComplaintStatus.VERIFIED
                && input.status() != ComplaintStatus.REOPENED)
            throw new SecurityException("Citizens may only verify or dispute a resolution");
        transitionInternal(c, input.status(), input.note(), actor);
        return c;
    }

    private void transitionInternal(Complaint c, ComplaintStatus next, String note, User actor) {
        if (!allowed(c.status, next))
            throw new IllegalArgumentException("Invalid transition from " + c.status + " to " + next);
        ComplaintStatus old = c.status;
        c.status = next;
        if (next == ComplaintStatus.RESOLVED)
            c.resolutionNotes = note;
        if (next == ComplaintStatus.REOPENED)
            c.resolutionNotes = null;
        c.updatedAt = Instant.now();
        complaints.save(c);
        record(c, old, next, note, actor);
        notify(c.citizen, "Complaint '" + c.title + "' is now " + next);
    }

    private boolean allowed(ComplaintStatus a, ComplaintStatus b) {
        return switch (a) {
            case SUBMITTED -> b == ComplaintStatus.ASSIGNED;
            case ASSIGNED -> b == ComplaintStatus.IN_PROGRESS;
            case IN_PROGRESS -> b == ComplaintStatus.RESOLVED;
            case RESOLVED -> b == ComplaintStatus.VERIFIED || b == ComplaintStatus.REOPENED;
            case REOPENED -> b == ComplaintStatus.IN_PROGRESS;
            case VERIFIED -> false;
        };
    }

    public Complaint get(UUID id) {
        return complaints.findById(id).orElseThrow(() -> new NoSuchElementException("Complaint not found"));
    }

    public List<Complaint> mine(User u) {
        return complaints.findByCitizenIdOrderByCreatedAtDesc(u.id);
    }

    public List<Complaint> department(User u) {
        return assignments.findByOfficialIdAndActiveTrue(u.id).stream()
                .flatMap(a -> complaints.findByDepartmentIdOrderByCreatedAtDesc(a.department.id).stream()).distinct()
                .toList();
    }

    public List<StatusHistory> history(UUID id) {
        return history.findByComplaintIdOrderByCreatedAtAsc(id);
    }

    private void record(Complaint c, ComplaintStatus from, ComplaintStatus to, String note, User actor) {
        StatusHistory h = new StatusHistory();
        h.complaint = c;
        h.fromStatus = from;
        h.toStatus = to;
        h.note = note;
        h.actor = actor;
        history.save(h);
    }

    private void notify(User u, String msg) {
        Notification n = new Notification();
        n.user = u;
        n.message = msg;
        notifications.save(n);
    }

    @Transactional
    public int markOverdue() {
        int count = 0;
        for (Complaint c : complaints.findBySlaDeadlineBeforeAndStatusNotIn(Instant.now(),
                List.of(ComplaintStatus.VERIFIED))) {
            if (!c.overdue) {
                c.overdue = true;
                complaints.save(c);
                if (c.assignedOfficial != null)
                    notify(c.assignedOfficial, "SLA overdue: " + c.title);
                count++;
            }
        }
        return count;
    }
}

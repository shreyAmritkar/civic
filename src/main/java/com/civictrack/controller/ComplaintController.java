package com.civictrack.controller;

import com.civictrack.dto.ComplaintDtos.*;
import com.civictrack.entity.*;
import com.civictrack.repository.*;
import com.civictrack.service.*;
import jakarta.validation.*;
import org.springframework.security.core.annotation.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {
    private final ComplaintService service;
    private final NotificationRepository notifications;
    private final AssignmentRepository assignments;

    public ComplaintController(ComplaintService s, NotificationRepository n, AssignmentRepository a) {
        service = s;
        notifications = n;
        assignments = a;
    }

    @PostMapping
    public Complaint create(@AuthenticationPrincipal User u, @Valid @RequestBody Create r) {
        return service.create(u, r);
    }

    @GetMapping
    public List<Complaint> mine(@AuthenticationPrincipal User u) {
        return service.mine(u);
    }

    @GetMapping("/{id}")
    public Complaint one(@PathVariable UUID id, @AuthenticationPrincipal User u) {
        Complaint c = service.get(id);
        if (u.role == Role.CITIZEN && !c.citizen.id.equals(u.id))
            throw new SecurityException("Not your complaint");
        if (u.role == Role.OFFICIAL && (c.department == null || assignments.findByOfficialIdAndActiveTrue(u.id).stream()
                .noneMatch(a -> a.department.id.equals(c.department.id))))
            throw new SecurityException("Not assigned to this department");
        return c;
    }

    @GetMapping("/{id}/history")
    public List<StatusHistory> history(@PathVariable UUID id, @AuthenticationPrincipal User u) {
        one(id, u);
        return service.history(id);
    }

    @PostMapping("/{id}/transition")
    public Complaint transition(@PathVariable UUID id, @AuthenticationPrincipal User u,
            @Valid @RequestBody Transition r) {
        return service.transition(id, r, u);
    }

    @GetMapping("/notifications")
    public List<Notification> notifications(@AuthenticationPrincipal User u) {
        return notifications.findByUserIdOrderByCreatedAtDesc(u.id);
    }
}

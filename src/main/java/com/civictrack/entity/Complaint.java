package com.civictrack.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "complaints")
public class Complaint {
    @Id
    public UUID id = UUID.randomUUID();
    @ManyToOne
    @JoinColumn(name = "citizen_id")
    public User citizen;
    @ManyToOne
    @JoinColumn(name = "department_id")
    public Department department;
    @ManyToOne
    @JoinColumn(name = "assigned_official_id")
    public User assignedOfficial;
    @Column(nullable = false)
    public String title;
    @Column(columnDefinition = "TEXT", nullable = false)
    public String description;
    @Column(nullable = false)
    public String category;
    @Enumerated(EnumType.STRING)
    public Priority priority = Priority.MEDIUM;
    @Column(nullable = false)
    public String location;
    public String ward;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ComplaintStatus status = ComplaintStatus.SUBMITTED;
    @Column(name = "assignment_status", nullable = false)
    public String assignmentStatus = "PENDING_REVIEW";
    @Column(name = "sla_deadline", nullable = false)
    public Instant slaDeadline;
    public boolean overdue;
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    public String resolutionNotes;
    @Column(name = "ai_status")
    public String aiStatus = "NOT_REQUESTED";
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    public String aiSummary;
    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
    @Column(name = "updated_at")
    public Instant updatedAt = Instant.now();
}

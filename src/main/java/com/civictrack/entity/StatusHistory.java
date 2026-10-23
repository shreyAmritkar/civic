package com.civictrack.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "complaint_status_history")
public class StatusHistory {
    @Id
    public UUID id = UUID.randomUUID();
    @ManyToOne
    @JoinColumn(name = "complaint_id")
    public Complaint complaint;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    public ComplaintStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    public ComplaintStatus toStatus;
    @Column(columnDefinition = "TEXT")
    public String note;
    @ManyToOne
    @JoinColumn(name = "actor_id")
    public User actor;
    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

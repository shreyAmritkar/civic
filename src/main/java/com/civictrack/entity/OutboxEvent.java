package com.civictrack.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    public UUID id = UUID.randomUUID();
    @Column(nullable = false)
    public String type;
    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id")
    public User recipient;
    @ManyToOne
    @JoinColumn(name = "complaint_id")
    public Complaint complaint;
    @Column(nullable = false)
    public String subject;
    @Column(columnDefinition = "TEXT", nullable = false)
    public String body;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OutboxStatus status = OutboxStatus.PENDING;
    public int attempts;
    @Column(name = "last_error", columnDefinition = "TEXT")
    public String lastError;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
    @Column(name = "dispatched_at")
    public Instant dispatchedAt;
    @Column(name = "sent_at")
    public Instant sentAt;
}
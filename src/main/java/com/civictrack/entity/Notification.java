package com.civictrack.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    public UUID id = UUID.randomUUID();
    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;
    @Column(columnDefinition = "TEXT", nullable = false)
    public String message;
    public boolean read;
    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

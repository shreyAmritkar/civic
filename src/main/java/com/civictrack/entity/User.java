package com.civictrack.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    public UUID id = UUID.randomUUID();
    @Column(nullable = false)
    public String name;
    @Column(nullable = false, unique = true)
    public String email;
    @Column(name = "password_hash", nullable = false)
    public String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Role role;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt = Instant.now();
}

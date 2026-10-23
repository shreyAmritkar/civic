package com.civictrack.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "departments")
public class Department {
    @Id
    public UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true)
    public String name;
    @Column(nullable = false)
    public String categories = "";
    public boolean active = true;
    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
}

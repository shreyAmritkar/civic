package com.civictrack.entity;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "official_assignments")
public class OfficialAssignment {
    @Id
    public UUID id = UUID.randomUUID();
    @ManyToOne
    @JoinColumn(name = "official_id")
    public User official;
    @ManyToOne
    @JoinColumn(name = "department_id")
    public Department department;
    public String ward;
    public boolean active = true;
}

package com.civictrack.controller;

import com.civictrack.dto.ComplaintDtos.*;
import com.civictrack.entity.*;
import com.civictrack.repository.*;
import com.civictrack.service.*;
import jakarta.validation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ComplaintRepository complaints;
    private final DepartmentRepository departments;
    private final AssignmentRepository assignments;
    private final UserRepository users;
    private final ComplaintService service;

    public AdminController(ComplaintRepository c, DepartmentRepository d, AssignmentRepository a, UserRepository u,
            ComplaintService s) {
        complaints = c;
        departments = d;
        assignments = a;
        users = u;
        service = s;
    }

    @GetMapping("/complaints")
    public List<Complaint> complaints() {
        return complaints.findAll();
    }

    @GetMapping("/departments")
    public List<Department> departments() {
        return departments.findAll();
    }

    @PostMapping("/departments")
    public Department department(@Valid @RequestBody DepartmentInput i) {
        Department d = new Department();
        d.name = i.name();
        d.categories = i.categories().toUpperCase();
        return departments.save(d);
    }

    @PostMapping("/assignments")
    public OfficialAssignment assignment(@Valid @RequestBody AssignmentInput i) {
        OfficialAssignment a = new OfficialAssignment();
        a.official = users.findById(i.officialId()).orElseThrow();
        a.department = departments.findById(i.departmentId()).orElseThrow();
        if (a.official.role != Role.OFFICIAL)
            throw new IllegalArgumentException("User must be an official");
        a.ward = i.ward();
        return assignments.save(a);
    }

    @PostMapping("/complaints/{id}/assign")
    public Complaint assign(@PathVariable UUID id, @Valid @RequestBody Assign i, @AuthenticationPrincipal User u) {
        return service.assign(id, i, u);
    }
}

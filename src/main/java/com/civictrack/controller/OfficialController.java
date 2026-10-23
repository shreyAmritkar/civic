package com.civictrack.controller;

import com.civictrack.dto.ComplaintDtos.*;
import com.civictrack.entity.*;
import com.civictrack.service.*;
import jakarta.validation.*;
import org.springframework.security.core.annotation.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/official")
public class OfficialController {
    private final ComplaintService service;

    public OfficialController(ComplaintService s) {
        service = s;
    }

    @GetMapping("/complaints")
    public List<Complaint> list(@AuthenticationPrincipal User u) {
        return service.department(u);
    }

    @PostMapping("/complaints/{id}/transition")
    public Complaint transition(@PathVariable UUID id, @AuthenticationPrincipal User u,
            @Valid @RequestBody Transition r) {
        return service.transition(id, r, u);
    }
}

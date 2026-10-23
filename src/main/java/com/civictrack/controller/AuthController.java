package com.civictrack.controller;

import com.civictrack.dto.AuthDtos.*;
import com.civictrack.service.*;
import jakarta.validation.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService s) {
        service = s;
    }

    @PostMapping("/register")
    public Token register(@Valid @RequestBody Register r) {
        return service.register(r);
    }

    @PostMapping("/login")
    public Token login(@Valid @RequestBody Login l) {
        return service.login(l);
    }
}

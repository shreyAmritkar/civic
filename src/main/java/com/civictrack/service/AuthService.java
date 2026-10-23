package com.civictrack.service;

import com.civictrack.dto.AuthDtos.*;
import com.civictrack.entity.*;
import com.civictrack.repository.*;
import com.civictrack.security.*;
import org.springframework.security.crypto.password.*;
import org.springframework.stereotype.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository u, PasswordEncoder e, JwtService j) {
        users = u;
        encoder = e;
        jwt = j;
    }

    public Token register(Register r) {
        if (users.findByEmail(r.email().toLowerCase()).isPresent())
            throw new IllegalArgumentException("Email is already registered");
        User u = new User();
        u.name = r.name();
        u.email = r.email().toLowerCase();
        u.passwordHash = encoder.encode(r.password());
        u.role = Role.CITIZEN;
        users.save(u);
        return token(u);
    }

    public Token login(Login l) {
        User u = users.findByEmail(l.email().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if (!encoder.matches(l.password(), u.passwordHash))
            throw new IllegalArgumentException("Invalid email or password");
        return token(u);
    }

    private Token token(User u) {
        return new Token(jwt.create(u), u.name, u.role.name());
    }
}

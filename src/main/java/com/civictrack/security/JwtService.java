package com.civictrack.security;

import com.civictrack.entity.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import java.nio.charset.*;
import java.util.*;

@Service
public class JwtService {
    private final byte[] key;
    private final long expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-ms}") long expiration) {
        this.key = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 32);
        this.expiration = expiration;
    }

    public String create(User u) {
        return Jwts.builder().subject(u.id.toString()).claim("role", u.role.name()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration)).signWith(Keys.hmacShaKeyFor(key))
                .compact();
    }

    public UUID userId(String token) {
        return UUID.fromString(Jwts.parser().verifyWith(Keys.hmacShaKeyFor(key)).build().parseSignedClaims(token)
                .getPayload().getSubject());
    }
}

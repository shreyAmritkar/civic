package com.civictrack.security;

import com.civictrack.repository.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.*;
import org.springframework.security.core.context.*;
import org.springframework.stereotype.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtFilter(JwtService j, UserRepository u) {
        jwt = j;
        users = u;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain c)
            throws ServletException, IOException {
        String h = r.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer "))
            try {
                var u = users.findById(jwt.userId(h.substring(7))).orElseThrow();
                var a = new UsernamePasswordAuthenticationToken(u, null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_" + u.role)));
                SecurityContextHolder.getContext().setAuthentication(a);
            } catch (Exception ignored) {
            }
        c.doFilter(r, s);
    }
}

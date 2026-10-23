package com.civictrack.config;

import com.civictrack.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.http.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.*;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain security(HttpSecurity h, JwtFilter f,
            @Value("${app.rate-limit.enabled:true}") boolean rateLimitEnabled) throws Exception {
        h.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        a -> a.requestMatchers("/", "/index.html", "/app.js", "/styles.css", "/api/auth/**").permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/official/**")
                                .hasAnyRole("OFFICIAL", "ADMIN").anyRequest().authenticated())
                .addFilterBefore(f, UsernamePasswordAuthenticationFilter.class);
        if (rateLimitEnabled)
            h.addFilterAfter(new RateLimitFilter(), JwtFilter.class);
        return h.build();
    }
}
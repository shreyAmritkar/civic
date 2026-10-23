package com.civictrack.config;
import jakarta.servlet.DispatcherType;
import com.civictrack.security.*;
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
    SecurityFilterChain security(HttpSecurity h, JwtFilter f) throws Exception {
        return h.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        a -> a.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                                .requestMatchers("/", "/index.html", "/app.js", "/styles.css","/dashboard", "/dashboard/**", "/api/auth/**").permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/official/**")
                                .hasAnyRole("OFFICIAL", "ADMIN").anyRequest().authenticated())
                .addFilterBefore(f, UsernamePasswordAuthenticationFilter.class).build();
    }
}

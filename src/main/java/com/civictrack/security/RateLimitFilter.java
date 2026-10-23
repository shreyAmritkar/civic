package com.civictrack.security;

import com.civictrack.entity.User;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Rate limits every /api request. Runs right after JwtFilter, so it can count per logged-in user;
 * anonymous requests (login, register) are counted per client IP.
 * The first matching rule wins. Change the numbers in RULES to tune.
 */
public class RateLimitFilter extends OncePerRequestFilter {
    private record Rule(String name, String method, Pattern path, int limit, Duration window, boolean perUser) {
        boolean matches(HttpServletRequest r) {
            return (method == null || method.equals(r.getMethod())) && path.matcher(r.getRequestURI()).matches();
        }
    }

    private static final List<Rule> RULES = List.of(
            // brute-force protection for passwords and account spam
            new Rule("login", "POST", Pattern.compile("/api/auth/login/?"), 10, Duration.ofMinutes(1), false),
            new Rule("register", "POST", Pattern.compile("/api/auth/register/?"), 10, Duration.ofHours(1), false),
            // each new complaint also queues an AI classification job
            new Rule("complaint-create", "POST", Pattern.compile("/api/complaints/?"), 10, Duration.ofHours(1), true),
            // each presign signs an S3 upload URL
            new Rule("media-presign", "POST", Pattern.compile("/api/complaints/[^/]+/media/presign/?"), 20,
                    Duration.ofMinutes(10), true),
            // everything else under /api
            new Rule("api", null, Pattern.compile("/api/.*"), 120, Duration.ofMinutes(1), true));

    private final RateLimiter limiter = new RateLimiter();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest r) {
        return !r.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest r, HttpServletResponse s, FilterChain chain)
            throws ServletException, IOException {
        Rule rule = RULES.stream().filter(x -> x.matches(r)).findFirst().orElse(null);
        if (rule != null) {
            RateLimiter.Result result = limiter.tryAcquire(rule.name() + "|" + clientKey(r, rule.perUser()),
                    rule.limit(), rule.window());
            if (!result.allowed()) {
                s.setStatus(429);
                s.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
                s.setContentType("application/json");
                s.setCharacterEncoding("UTF-8");
                s.getWriter().write("{\"error\":\"Too many requests. Please try again in "
                        + result.retryAfterSeconds() + " seconds.\"}");
                return;
            }
        }
        chain.doFilter(r, s);
    }

    private static String clientKey(HttpServletRequest r, boolean perUser) {
        if (perUser) {
            Authentication a = SecurityContextHolder.getContext().getAuthentication();
            if (a != null && a.getPrincipal() instanceof User u)
                return "user:" + u.id;
        }
        return "ip:" + r.getRemoteAddr();
    }
}
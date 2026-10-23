package com.civictrack.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory token bucket limiter. No Redis or extra library needed.
 * A bucket holds up to "limit" tokens and refills evenly over "window", so a client may burst
 * up to the limit and is then held to limit / window on average.
 * Note: the counters live in this JVM only. Run more than one app instance and each has its own.
 */
public class RateLimiter {
    public record Result(boolean allowed, long retryAfterSeconds) {
    }

    private static class Bucket {
        double tokens;
        long lastRefill;
        long idleAfter;

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastRefill = now;
        }
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.nanoTime());

    public Result tryAcquire(String key, int limit, Duration window) {
        long now = System.nanoTime();
        cleanup(now);
        double ratePerNano = (double) limit / window.toNanos();
        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket(limit, now));
        synchronized (b) {
            b.tokens = Math.min(limit, b.tokens + (now - b.lastRefill) * ratePerNano);
            b.lastRefill = now;
            b.idleAfter = window.toNanos();
            if (b.tokens >= 1) {
                b.tokens -= 1;
                return new Result(true, 0);
            }
            long waitNanos = (long) ((1 - b.tokens) / ratePerNano);
            return new Result(false, Math.max(1, (waitNanos + 999_999_999L) / 1_000_000_000L));
        }
    }

    /** Once a minute, drop buckets that have been idle for a full window. They would be full again anyway. */
    private void cleanup(long now) {
        long last = lastCleanup.get();
        if (now - last < 60_000_000_000L || !lastCleanup.compareAndSet(last, now))
            return;
        buckets.entrySet().removeIf(e -> {
            Bucket b = e.getValue();
            synchronized (b) {
                return now - b.lastRefill > b.idleAfter;
            }
        });
    }
}
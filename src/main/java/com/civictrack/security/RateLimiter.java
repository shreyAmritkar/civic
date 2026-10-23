package com.civictrack.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter backed by Bucket4j (token bucket). One bucket per key, e.g. "login|ip:1.2.3.4".
 * A bucket holds up to "limit" tokens and refills evenly over "window", so a client may burst
 * up to the limit and is then held to limit / window on average.
 * The buckets live in this JVM only. To share them between containers, switch to Bucket4j's Redis
 * module (ProxyManager); the rest of the application does not need to change.
 */
public class RateLimiter {
    public record Result(boolean allowed, long retryAfterSeconds) {
    }

    private static class Entry {
        final Bucket bucket;
        final long idleAfterNanos;
        volatile long lastUsed;

        Entry(int limit, Duration window, long now) {
            bucket = Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(limit).refillGreedy(limit, window).build()).build();
            idleAfterNanos = window.toNanos();
            lastUsed = now;
        }
    }

    private final ConcurrentHashMap<String, Entry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong(System.nanoTime());

    public Result tryAcquire(String key, int limit, Duration window) {
        long now = System.nanoTime();
        cleanup(now);
        Entry entry = buckets.computeIfAbsent(key, k -> new Entry(limit, window, now));
        entry.lastUsed = now;
        ConsumptionProbe probe = entry.bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed())
            return new Result(true, 0);
        long seconds = (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L;
        return new Result(false, Math.max(1, seconds));
    }

    /** Bucket4j never forgets a bucket by itself. Once a minute, drop those idle for a full window (they are full again anyway). */
    private void cleanup(long now) {
        long last = lastCleanup.get();
        if (now - last < 60_000_000_000L || !lastCleanup.compareAndSet(last, now))
            return;
        buckets.values().removeIf(e -> now - e.lastUsed > e.idleAfterNanos);
    }
}
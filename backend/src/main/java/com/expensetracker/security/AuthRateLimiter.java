package com.expensetracker.security;

import com.expensetracker.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory fixed-window limiter for the public auth endpoints
 * (brute-force / credential-stuffing protection).
 *
 * <p>Callers {@link #assertAllowed} before doing the work, then
 * {@link #recordAttempt} for every attempt that should count (failed
 * logins, every registration) and {@link #reset} after a successful login.
 *
 * <p>State is per JVM instance. That is fine for the current single-node
 * deployment; a multi-instance deployment would move this to Redis.
 */
@Component
public class AuthRateLimiter {

    /** Purge expired windows once the map grows past this many keys. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Window(Instant start, int count) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;

    @Autowired
    public AuthRateLimiter(@Value("${app.auth.rate-limit.max-attempts:10}") int maxAttempts,
                           @Value("${app.auth.rate-limit.window-seconds:300}") long windowSeconds) {
        this(maxAttempts, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    AuthRateLimiter(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    /** Throws HTTP 429 if {@code key} has used up its attempts in the current window. */
    public void assertAllowed(String key) {
        Window w = windows.get(key);
        if (w != null && !isExpired(w, clock.instant()) && w.count() >= maxAttempts) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Please wait a few minutes and try again.");
        }
    }

    /** Counts one attempt against {@code key}. */
    public void recordAttempt(String key) {
        Instant now = clock.instant();
        windows.compute(key, (k, w) -> (w == null || isExpired(w, now))
                ? new Window(now, 1)
                : new Window(w.start(), w.count() + 1));
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> isExpired(w, now));
        }
    }

    /** Clears the counter for {@code key} (e.g. after a successful login). */
    public void reset(String key) {
        windows.remove(key);
    }

    private boolean isExpired(Window w, Instant now) {
        return !now.isBefore(w.start().plus(window));
    }
}

package com.expensetracker.security;

import com.expensetracker.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimiterTest {

    /** Mutable clock so tests can move time forward. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public Instant instant() { return now; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    private final TestClock clock = new TestClock();
    private final AuthRateLimiter limiter = new AuthRateLimiter(3, Duration.ofMinutes(5), clock);

    @Test
    @DisplayName("allows attempts up to the limit, then answers 429")
    void blocksAfterLimit() {
        for (int i = 0; i < 3; i++) {
            limiter.assertAllowed("k");
            limiter.recordAttempt("k");
        }
        assertThatThrownBy(() -> limiter.assertAllowed("k"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("keys are independent")
    void keysAreIndependent() {
        for (int i = 0; i < 3; i++) limiter.recordAttempt("a");
        assertThatCode(() -> limiter.assertAllowed("b")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the window expires and the counter starts over")
    void windowExpires() {
        for (int i = 0; i < 3; i++) limiter.recordAttempt("k");
        clock.advance(Duration.ofMinutes(5));
        assertThatCode(() -> limiter.assertAllowed("k")).doesNotThrowAnyException();
        limiter.recordAttempt("k");
        assertThatCode(() -> limiter.assertAllowed("k")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("reset clears the counter (successful login)")
    void resetClears() {
        for (int i = 0; i < 3; i++) limiter.recordAttempt("k");
        limiter.reset("k");
        assertThatCode(() -> limiter.assertAllowed("k")).doesNotThrowAnyException();
    }
}

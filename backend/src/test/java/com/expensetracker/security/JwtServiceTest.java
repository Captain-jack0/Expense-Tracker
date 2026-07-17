package com.expensetracker.security;

import com.expensetracker.exception.AuthException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long-for-hs256-signing";
    private static final long HOUR = 3_600_000L;
    private static final long WEEK = 604_800_000L;

    private final JwtService jwtService = new JwtService(SECRET, HOUR, WEEK);

    @Test
    @DisplayName("access token round-trips back to the same user id")
    void accessToken_roundTrip() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId);
        assertThat(jwtService.parseAccessToken(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("refresh token round-trips back to the same user id")
    void refreshToken_roundTrip() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateRefreshToken(userId);
        assertThat(jwtService.parseRefreshToken(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("an access token is rejected where a refresh token is expected")
    void accessToken_notAcceptedAsRefresh() {
        String access = jwtService.generateAccessToken(UUID.randomUUID());
        assertThatThrownBy(() -> jwtService.parseRefreshToken(access))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a refresh token is rejected where an access token is expected")
    void refreshToken_notAcceptedAsAccess() {
        String refresh = jwtService.generateRefreshToken(UUID.randomUUID());
        assertThatThrownBy(() -> jwtService.parseAccessToken(refresh))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("a malformed token is rejected")
    void malformedToken_rejected() {
        assertThatThrownBy(() -> jwtService.parseAccessToken("not-a-jwt"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a token signed with a different key is rejected")
    void wrongSignature_rejected() {
        JwtService other = new JwtService("another-different-secret-key-at-least-32-bytes-xx", HOUR, WEEK);
        String foreign = other.generateAccessToken(UUID.randomUUID());
        assertThatThrownBy(() -> jwtService.parseAccessToken(foreign))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredToken_rejected() {
        JwtService shortLived = new JwtService(SECRET, -1_000L, -1_000L); // already expired
        String expired = shortLived.generateAccessToken(UUID.randomUUID());
        assertThatThrownBy(() -> shortLived.parseAccessToken(expired))
                .isInstanceOf(AuthException.class);
    }
}

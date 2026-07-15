package com.expensetracker.security;

import com.expensetracker.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates HS256-signed JWTs. Two token types are produced:
 * "access" (short-lived, sent as a Bearer header) and "refresh"
 * (long-lived, exchanged at /auth/refresh). The token {@code type} claim
 * prevents a refresh token from being accepted where an access token is
 * required and vice versa.
 */
@Service
public class JwtService {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(UUID userId) {
        return buildToken(userId, TYPE_ACCESS, accessExpirationMs);
    }

    public String generateRefreshToken(UUID userId) {
        return buildToken(userId, TYPE_REFRESH, refreshExpirationMs);
    }

    /** Validates an access token and returns the user id it was issued for. */
    public UUID parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    /** Validates a refresh token and returns the user id it was issued for. */
    public UUID parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private String buildToken(UUID userId, String type, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    private UUID parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid token type");
            }
            return UUID.fromString(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }
}

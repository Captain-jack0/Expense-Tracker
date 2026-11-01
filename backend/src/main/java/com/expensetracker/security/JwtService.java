package com.expensetracker.security;

import com.expensetracker.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 *
 * <p>Refresh tokens also carry the user's {@code ver} (token version).
 * {@code AuthService} rejects a refresh token whose version no longer
 * matches the user's current one, which is how logout revokes them.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Prefix of the public, dev-only fallback key in application.properties. */
    static final String DEV_SECRET_PREFIX = "dev-only-";

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_VERSION = "ver";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    /** Subject + token version extracted from a validated refresh token. */
    public record RefreshClaims(UUID userId, int tokenVersion) {}

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        if (secret.startsWith(DEV_SECRET_PREFIX)) {
            log.warn("JWT tokens are being signed with the PUBLIC dev-only key. "
                    + "Set the APP_JWT_SECRET environment variable for any shared or deployed environment.");
        }
        // Keys.hmacShaKeyFor throws WeakKeyException for keys shorter than 256 bits.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(UUID userId) {
        return baseToken(userId, TYPE_ACCESS, accessExpirationMs).compact();
    }

    public String generateRefreshToken(UUID userId, int tokenVersion) {
        return baseToken(userId, TYPE_REFRESH, refreshExpirationMs)
                .claim(CLAIM_VERSION, tokenVersion)
                .compact();
    }

    /** Validates an access token and returns the user id it was issued for. */
    public UUID parseAccessToken(String token) {
        return UUID.fromString(parse(token, TYPE_ACCESS).getSubject());
    }

    /** Validates a refresh token and returns its user id and token version. */
    public RefreshClaims parseRefreshToken(String token) {
        Claims claims = parse(token, TYPE_REFRESH);
        Integer version = claims.get(CLAIM_VERSION, Integer.class);
        if (version == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return new RefreshClaims(UUID.fromString(claims.getSubject()), version);
    }

    private io.jsonwebtoken.JwtBuilder baseToken(UUID userId, String type, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key);
    }

    private Claims parse(String token, String expectedType) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            UUID.fromString(claims.getSubject()); // reject malformed subjects up front
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid token type");
        }
        return claims;
    }
}

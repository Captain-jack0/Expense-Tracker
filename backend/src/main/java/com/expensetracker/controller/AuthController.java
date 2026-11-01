package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import com.expensetracker.dto.AuthResponse;
import com.expensetracker.dto.LoginRequest;
import com.expensetracker.dto.RefreshRequest;
import com.expensetracker.dto.RegisterRequest;
import com.expensetracker.dto.UserDto;
import com.expensetracker.exception.AuthException;
import com.expensetracker.security.AuthRateLimiter;
import com.expensetracker.security.CurrentUser;
import com.expensetracker.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;

/**
 * Authentication endpoints. All responses use the {@link ApiResponse}
 * envelope expected by the frontend.
 *
 * <p>Login and register are rate limited per client IP (login additionally
 * per email) by {@link AuthRateLimiter}; exceeding the limit returns 429.
 * The client IP is the socket peer address. If the API is ever deployed
 * behind a reverse proxy, enable {@code server.forward-headers-strategy}
 * so this reflects the real client rather than the proxy.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimiter rateLimiter;

    public AuthController(AuthService authService, AuthRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest http) {
        String key = "register:" + http.getRemoteAddr();
        rateLimiter.assertAllowed(key);
        rateLimiter.recordAttempt(key);
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest http) {
        String key = "login:" + http.getRemoteAddr() + ":" + request.getEmail().trim().toLowerCase(Locale.ROOT);
        rateLimiter.assertAllowed(key);
        try {
            AuthResponse response = authService.login(request);
            rateLimiter.reset(key);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (AuthException ex) {
            rateLimiter.recordAttempt(key);
            throw ex;
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(@CurrentUser UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getCurrentUser(userId)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.getRefreshToken())));
    }

    /**
     * Revokes all of the caller's refresh tokens (server side) — the client
     * still discards its stored tokens. The short-lived access token stays
     * valid until it expires.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@CurrentUser UUID userId) {
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out"));
    }
}

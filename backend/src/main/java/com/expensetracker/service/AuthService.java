package com.expensetracker.service;

import com.expensetracker.dto.AuthResponse;
import com.expensetracker.dto.LoginRequest;
import com.expensetracker.dto.RegisterRequest;
import com.expensetracker.dto.UserDto;
import com.expensetracker.exception.AuthException;
import com.expensetracker.model.User;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Authentication business logic: registration, login, current-user lookup,
 * token refresh and logout.
 *
 * <p>Access tokens are stateless JWTs. Refresh tokens embed the user's
 * {@code tokenVersion}; {@link #logout} increments it, which revokes every
 * refresh token issued before that point.
 */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategorySeeder categorySeeder;

    /**
     * BCrypt hash compared against when the email is unknown, so a login for
     * a non-existent account costs the same time as a wrong password and the
     * response time does not reveal which emails are registered.
     */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       CategorySeeder categorySeeder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.categorySeeder = categorySeeder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setCurrency("USD");
        user.setTimezone("UTC");
        user.setActive(true);
        user.setEmailVerified(false);
        user.setTokenVersion(0);

        User saved = userRepository.save(user);
        // Seed the default categories inside the registration transaction so
        // a new user never hits the lazy (first-list) seeding path.
        categorySeeder.seedFor(saved.getId());
        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> found = userRepository.findByEmail(email);

        String hash = found.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), hash);

        if (found.isEmpty() || !passwordMatches) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        return buildAuthResponse(found.get());
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        return UserDto.from(requireUser(userId));
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        JwtService.RefreshClaims claims = jwtService.parseRefreshToken(refreshToken);
        User user = requireUser(claims.userId());
        if (claims.tokenVersion() != user.getTokenVersion()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }
        return buildAuthResponse(user);
    }

    /** Revokes every refresh token issued to the user so far. */
    @Transactional
    public void logout(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);
        });
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getTokenVersion());
        return new AuthResponse(UserDto.from(user), accessToken, refreshToken);
    }
}

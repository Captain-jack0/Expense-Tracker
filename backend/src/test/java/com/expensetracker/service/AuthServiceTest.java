package com.expensetracker.service;

import com.expensetracker.dto.AuthResponse;
import com.expensetracker.dto.LoginRequest;
import com.expensetracker.dto.RegisterRequest;
import com.expensetracker.dto.UserDto;
import com.expensetracker.exception.AuthException;
import com.expensetracker.model.User;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
        lenient().when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        lenient().when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
    }

    private static User persistedUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setPasswordHash("hashed");
        u.setFirstName("Ada");
        u.setLastName("Lovelace");
        u.setCurrency("USD");
        u.setTimezone("UTC");
        u.setActive(true);
        u.setEmailVerified(false);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        return u;
    }

    private static RegisterRequest registerRequest(String email) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPassword("correcthorse");
        r.setFirstName("Ada");
        r.setLastName("Lovelace");
        return r;
    }

    @Test
    @DisplayName("register normalises the email, hashes the password and returns tokens")
    void register_success() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("correcthorse")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(Instant.now());
            u.setUpdatedAt(Instant.now());
            return u;
        });

        AuthResponse response = authService.register(registerRequest("  ADA@Example.com "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    @DisplayName("register rejects a duplicate email with 409")
    void register_duplicateEmail_conflict() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("ada@example.com")))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login succeeds with valid credentials")
    void login_success() {
        User user = persistedUser("ada@example.com");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correcthorse", "hashed")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setEmail("ada@example.com");
        req.setPassword("correcthorse");

        AuthResponse response = authService.login(req);
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser().getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    @DisplayName("login with an unknown email fails with 401")
    void login_unknownEmail_unauthorized() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@example.com");
        req.setPassword("x");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("login with a wrong password fails with 401")
    void login_wrongPassword_unauthorized() {
        User user = persistedUser("ada@example.com");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("ada@example.com");
        req.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("getCurrentUser returns the user when found, else 401")
    void getCurrentUser() {
        User user = persistedUser("ada@example.com");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        UserDto dto = authService.getCurrentUser(user.getId());
        assertThat(dto.getEmail()).isEqualTo("ada@example.com");

        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.getCurrentUser(missing)).isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("refresh issues a fresh token pair for a valid refresh token")
    void refresh_success() {
        User user = persistedUser("ada@example.com");
        when(jwtService.parseRefreshToken("refresh-token")).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AuthResponse response = authService.refresh("refresh-token");
        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("refresh fails when the token's user no longer exists")
    void refresh_userGone_unauthorized() {
        UUID id = UUID.randomUUID();
        when(jwtService.parseRefreshToken("refresh-token")).thenReturn(id);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("refresh-token")).isInstanceOf(AuthException.class);
    }
}

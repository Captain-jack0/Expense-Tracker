package com.expensetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload returned by /auth/login, /auth/register and /auth/refresh.
 * Matches the frontend {@code AuthResponse} type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private UserDto user;
    private String accessToken;
    private String refreshToken;
}

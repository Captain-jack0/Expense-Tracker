package com.expensetracker.exception;

import org.springframework.http.HttpStatus;

/**
 * Authentication/authorization failure (bad credentials, duplicate email,
 * invalid/expired token). Specialisation of {@link ApiException} kept for
 * semantic clarity at call sites.
 */
public class AuthException extends ApiException {

    public AuthException(HttpStatus status, String message) {
        super(status, message);
    }
}

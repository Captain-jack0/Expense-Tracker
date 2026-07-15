package com.expensetracker.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds the authenticated user's id (UUID) to a controller method parameter,
 * resolved from the Bearer access token by {@link CurrentUserArgumentResolver}.
 *
 * <pre>{@code
 * @GetMapping("/categories")
 * public ... list(@CurrentUser UUID userId) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}

package com.expensetracker.dto;

import com.expensetracker.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User representation returned to the client. Deliberately excludes the
 * password hash. JSON keys match the frontend {@code User} type exactly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String currency;
    private String timezone;

    // Explicit @JsonProperty so the boolean serializes as "isActive"
    // (Jackson would otherwise strip the "is" prefix and emit "active").
    @JsonProperty("isActive")
    private boolean isActive;

    @JsonProperty("emailVerified")
    private boolean emailVerified;

    private String createdAt;
    private String updatedAt;

    public static UserDto from(User user) {
        return new UserDto(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCurrency(),
                user.getTimezone(),
                user.isActive(),
                user.isEmailVerified(),
                user.getCreatedAt().toString(),
                user.getUpdatedAt().toString()
        );
    }
}

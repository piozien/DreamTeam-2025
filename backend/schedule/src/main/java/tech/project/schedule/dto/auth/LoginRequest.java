package tech.project.schedule.dto.auth;


/**
 * Data Transfer Object used for user login requests.
 * Contains the credentials (email and password) needed to authenticate a user.
 */

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object used for user login requests.
 * Contains the credentials (email and password) needed to authenticate a user.
 */
public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password
) {}


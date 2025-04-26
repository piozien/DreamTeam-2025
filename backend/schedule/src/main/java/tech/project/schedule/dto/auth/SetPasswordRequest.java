package tech.project.schedule.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/*
 * Data Transfer Object for setting a new password after registration.
 */
/**
 * Data Transfer Object for setting a new password after registration or reset.
 */
public record SetPasswordRequest(
    @Email @NotBlank String email,
    @NotBlank String newPassword
) {}


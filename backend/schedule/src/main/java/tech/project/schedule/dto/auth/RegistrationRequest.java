package tech.project.schedule.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import tech.project.schedule.model.enums.GlobalRole;

/**
 * Data Transfer Object that contains user information needed for registration.
 * This class captures all required fields for creating a new user account in the system.
 */
/**
 * Data Transfer Object that contains user information needed for registration.
 * This class captures all required fields for creating a new user account in the system.
 */
public record RegistrationRequest(
    @NotBlank String username,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank String password,
    @Email @NotBlank String email,
    GlobalRole role
) {}


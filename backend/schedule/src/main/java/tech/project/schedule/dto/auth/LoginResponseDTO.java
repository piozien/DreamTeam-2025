package tech.project.schedule.dto.auth;

import java.util.UUID;

/**
 * Data Transfer Object that contains user information returned after successful login.
 * This immutable record provides essential user identification details to the client
 * following authentication.
 */
public record LoginResponseDTO(
        UUID id,
        String email,
        String name,
        String username
) {
}

package tech.project.schedule.dto.auth;

/**
 * Data Transfer Object that contains the response message for user registration attempts.
 * Returns a simple status message indicating registration success or failure.
 */
public record RegistrationResponseDTO(
        String message
) {
}

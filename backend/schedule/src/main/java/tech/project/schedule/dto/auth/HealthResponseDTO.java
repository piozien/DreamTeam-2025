package tech.project.schedule.dto.auth;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for system health check responses.
 * Contains basic information about system status and the time when the check was performed.
 */
public record HealthResponseDTO(
        String status,
        LocalDateTime timestamp
) {
}

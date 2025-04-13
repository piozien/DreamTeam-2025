package tech.project.schedule.dto.auth;

import java.time.LocalDateTime;

public record HealthResponseDTO(
        String status,
        LocalDateTime timestamp
) {
}

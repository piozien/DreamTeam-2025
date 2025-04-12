package tech.project.schedule.dto.auth;

import java.util.UUID;

public record LoginResponseDTO(
        UUID id,
        String email,
        String name,
        String username
) {
}

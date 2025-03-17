package tech.project.schedule.dto.project;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProjectDTO(
        UUID id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        Set<UUID> memberIds,
        Set<UUID> taskIds
) {
}

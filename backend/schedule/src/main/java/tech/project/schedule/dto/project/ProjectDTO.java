package tech.project.schedule.dto.project;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record ProjectDTO(
        String name,
        String description,
        LocalDate startDate
) {
}

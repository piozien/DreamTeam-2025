package tech.project.schedule.dto.project;

import tech.project.schedule.model.enums.ProjectStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ProjectUpdateDTO(
        UUID id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        ProjectStatus projectStatus
) {}

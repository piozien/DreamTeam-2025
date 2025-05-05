package tech.project.schedule.dto.project;

import tech.project.schedule.model.enums.ProjectStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object specifically designed for project update operations.
 * Contains fields that can be modified when updating an existing project.
 * Unlike the creation DTO, fields can be null to support partial updates.
 */
public record ProjectUpdateDTO(
        UUID id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        ProjectStatus projectStatus
) {}

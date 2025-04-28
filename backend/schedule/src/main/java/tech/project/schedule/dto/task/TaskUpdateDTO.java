package tech.project.schedule.dto.task;

import tech.project.schedule.model.enums.TaskPriority;
import tech.project.schedule.model.enums.TaskStatus;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object specifically designed for task update operations.
 * Contains all modifiable fields of a task to support partial or complete updates.
 * Unlike the task creation DTO, validation constraints are not applied since
 * fields may be intentionally null when not being updated.
 */
public record TaskUpdateDTO(
        UUID id,
        UUID projectId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        TaskPriority priority,
        TaskStatus status,
        Set<UUID> assigneeIds,
        Set<TaskCommentDTO> comments,
        Set<TaskFileDTO> files,
        Set<UUID> dependencyIds
) {}

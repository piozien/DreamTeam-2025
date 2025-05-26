package tech.project.schedule.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.project.schedule.model.enums.TaskPriority;
import tech.project.schedule.model.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object specifically designed for task creation requests.
 * Contains all the necessary fields required to create a new task in the system,
 * with validation constraints applied to required fields.
 */
public record TaskRequestDTO(
        @NotNull(message = "Project ID is required")
        UUID projectId,

        @NotBlank(message = "Task name is required")
        String name,

        String description,

        @NotNull(message = "Start date is required")
        LocalDateTime startDate,

        LocalDateTime endDate,

        @NotNull(message = "Priority is required")
        TaskPriority priority,

        @NotNull(message = "Status is required")
        TaskStatus status,

        Set<UUID> assigneeIds,
        Set<TaskCommentDTO> comments,
        Set<UUID> dependencyIds
) {}

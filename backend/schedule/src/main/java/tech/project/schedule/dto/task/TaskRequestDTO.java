package tech.project.schedule.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.project.schedule.model.enums.TaskPriority;
import tech.project.schedule.model.enums.TaskStatus;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;


public record TaskRequestDTO(
        @NotNull(message = "Project ID is required")
        UUID projectId,

        @NotBlank(message = "Task name is required")
        String name,

        String description,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        @NotNull(message = "Priority is required")
        TaskPriority priority,

        @NotNull(message = "Status is required")
        TaskStatus status,

        Set<UUID> assigneeIds,
        Set<TaskCommentDTO> comments,
        Set<TaskFileDTO> files,
        Set<UUID> dependencyIds
) {}

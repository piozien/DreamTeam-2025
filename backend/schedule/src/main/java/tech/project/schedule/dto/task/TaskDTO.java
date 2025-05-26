package tech.project.schedule.dto.task;

import tech.project.schedule.model.enums.TaskPriority;
import tech.project.schedule.model.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object that represents a task in the scheduling system.
 * Contains comprehensive task information including identification, scheduling details,
 * priority and status, as well as relationships to assigned users, comments,
 * and task dependencies.
 */
public record TaskDTO(
        UUID id,
        UUID projectId,
        String name,
        String description,
        LocalDateTime startDate,
        LocalDateTime endDate,
        TaskPriority priority,
        TaskStatus status,
        Set<UUID> assigneeIds,
        Set<TaskCommentDTO> comments,
        Set<UUID> dependencyIds
) {
}

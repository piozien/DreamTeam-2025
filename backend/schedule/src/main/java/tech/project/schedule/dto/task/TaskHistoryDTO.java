package tech.project.schedule.dto.task;

import java.time.LocalDateTime;
import java.util.UUID;
import tech.project.schedule.model.enums.TaskStatus;

/**
 * Data Transfer Object that represents a historical record of task status changes.
 * Used for tracking the progression of tasks through various statuses, capturing
 * who made each change and when it occurred.
 */
public record TaskHistoryDTO(
        UUID id,
        UUID taskId,
        UUID changedById,
        TaskStatus oldStatus,
        TaskStatus newStatus,
        LocalDateTime changedAt
) {}

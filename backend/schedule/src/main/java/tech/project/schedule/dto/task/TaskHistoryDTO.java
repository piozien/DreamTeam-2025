package tech.project.schedule.dto.task;

import java.time.LocalDateTime;
import java.util.UUID;
import tech.project.schedule.model.enums.TaskStatus;

public record TaskHistoryDTO(
        UUID id,
        UUID taskId,
        UUID changedById,
        TaskStatus oldStatus,
        TaskStatus newStatus,
        LocalDateTime changedAt
) {}
package tech.project.schedule.dto.task;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskCommentDTO(
        UUID id,
        UUID taskId,
        UUID userId,
        String comment,
        LocalDateTime createdAt
) {
}

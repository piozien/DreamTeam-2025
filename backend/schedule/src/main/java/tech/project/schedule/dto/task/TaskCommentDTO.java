package tech.project.schedule.dto.task;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object that represents a comment on a task.
 * Contains the comment text, its identifiers, associated user information,
 * and timestamp data for when the comment was created.
 */
public record TaskCommentDTO(
        UUID id,
        UUID taskId,
        UUID userId,
        String comment,
        LocalDateTime createdAt
) {
}

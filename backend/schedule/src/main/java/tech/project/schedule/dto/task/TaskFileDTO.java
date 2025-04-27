package tech.project.schedule.dto.task;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a file attached to a task.
 * Contains metadata about uploaded files including the file location,
 * upload information, and associated identifiers.
 */
public record TaskFileDTO(
        UUID id,
        UUID taskId,
        UUID uploadedById,
        String filePath,
        LocalDateTime uploadedAt
) {}

package tech.project.schedule.dto.task;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskFileDTO(
        UUID id,
        UUID taskId,
        UUID uploadedById,
        String filePath,
        LocalDateTime uploadedAt
) {}
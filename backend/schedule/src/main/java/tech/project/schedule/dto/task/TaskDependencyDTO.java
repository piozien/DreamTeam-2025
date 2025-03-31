package tech.project.schedule.dto.task;

import java.util.UUID;

public record TaskDependencyDTO(
        UUID id,
        UUID taskId,
        UUID dependsOnTaskId
) {}
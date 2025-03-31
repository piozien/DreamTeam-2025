package tech.project.schedule.dto.task;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import tech.project.schedule.model.enums.TaskPriority;
import tech.project.schedule.model.enums.TaskStatus;

public record TaskDTO(
        UUID id,
        UUID projectId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        TaskPriority priority,
        TaskStatus status,
        Set<UUID> assigneeIds,
        Set<UUID> commentIds,
        Set<UUID> historyIds,
        Set<UUID> fileIds,
        Set<UUID> dependencyIds,
        Set<UUID> dependentTaskIds
) {
}

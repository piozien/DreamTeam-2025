package tech.project.schedule.dto.task;

import tech.project.schedule.model.enums.TaskPriority;
import tech.project.schedule.model.enums.TaskStatus;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record TaskUpdateDTO(
        UUID id,
        UUID projectId,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        TaskPriority priority,
        TaskStatus status,
        Set<UUID> assigneeIds,
        Set<TaskCommentDTO> comments,
        Set<TaskFileDTO> files,
        Set<UUID> dependencyIds
) {}

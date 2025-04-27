package tech.project.schedule.dto.task;

import java.util.UUID;

/**
 * Data Transfer Object that represents a dependency relationship between tasks.
 * Contains the identifiers necessary to track which task depends on another task,
 * establishing prerequisite relationships in the task workflow.
 */
public record TaskDependencyDTO(
        UUID id,
        UUID taskId,
        UUID dependsOnTaskId
) {}

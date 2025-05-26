package tech.project.schedule.dto.task;

import java.util.UUID;

/**
 * Data Transfer Object that represents the assignment of a user to a task.
 * Contains the IDs needed to identify the assignment relationship between a user and a task.
 */
public record TaskAssigneeDTO (
        UUID id,
        UUID taskId,
        UUID userId
){
}

package tech.project.schedule.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for updating a task dependency relationship.
 * Used when replacing one prerequisite task with another for a specific task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskDependencyRequest {
    private UUID newDependentOnTaskId;
}

package tech.project.schedule.dto.task;

import java.util.UUID;

public record TaskAssigneeDTO (
        UUID id,
        UUID taskId,
        UUID userId
){
}

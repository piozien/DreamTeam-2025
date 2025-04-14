package tech.project.schedule.dto.project;

import java.util.UUID;
import tech.project.schedule.model.enums.ProjectUserRole;

public record ProjectMemberDTO(
        UUID id,
        UUID projectId,
        UUID userId,
        String name, ProjectUserRole role
) {
}

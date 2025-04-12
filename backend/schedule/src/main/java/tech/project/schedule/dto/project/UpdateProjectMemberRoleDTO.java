package tech.project.schedule.dto.project;

import jakarta.validation.constraints.NotNull;
import tech.project.schedule.model.enums.ProjectUserRole;

public record UpdateProjectMemberRoleDTO(
        @NotNull(message = "Role is required")
        ProjectUserRole role
) {
}

package tech.project.schedule.dto.project;

import jakarta.validation.constraints.NotNull;
import tech.project.schedule.model.enums.ProjectUserRole;

/**
 * Data Transfer Object used for updating a project member's role.
 * Contains only the role field which is required for the update operation.
 */
public record UpdateProjectMemberRoleDTO(
        @NotNull(message = "Role is required")
        ProjectUserRole role
) {
}

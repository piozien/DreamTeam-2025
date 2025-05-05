package tech.project.schedule.dto.project;

import jakarta.validation.constraints.NotNull;
import tech.project.schedule.model.enums.ProjectUserRole;

import java.util.UUID;

/**
 * Data Transfer Object used for adding a new member to a project.
 * Contains the user ID and the role they should be assigned within the project.
 * Both fields are required.
 */
public record AddProjectMemberDTO(
        @NotNull(message = "User ID is required")
        UUID userId,
        
        @NotNull(message = "Role is required")
        ProjectUserRole role
) {
}

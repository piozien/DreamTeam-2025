package tech.project.schedule.dto.project;

import jakarta.validation.constraints.NotNull;
import tech.project.schedule.model.enums.ProjectUserRole;

import java.util.UUID;

public record AddProjectMemberDTO(
        @NotNull(message = "User ID is required")
        UUID userId,
        
        @NotNull(message = "Role is required")
        ProjectUserRole role
) {
}

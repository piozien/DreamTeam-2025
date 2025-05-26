package tech.project.schedule.dto.project;

import java.util.UUID;
import tech.project.schedule.model.enums.ProjectUserRole;

/**
 * Data Transfer Object that represents a member of a project.
 * Contains information about the membership relationship including the user's 
 * identification, project identification, and their assigned role in the project.
 */
public record ProjectMemberDTO(
        UUID id,
        UUID projectId,
        UUID userId,
        String name, ProjectUserRole role
) {
}

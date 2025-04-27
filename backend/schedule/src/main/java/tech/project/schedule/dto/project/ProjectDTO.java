package tech.project.schedule.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.project.schedule.model.enums.ProjectStatus;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object that represents a project in the scheduling system.
 * Contains project details including identification, scheduling information,
 * associated members and tasks. The required fields are name and start date.
 */
public record ProjectDTO(
        UUID id,
        
        @NotBlank(message = "Project name is required")
        String name,
        
        String description,
        
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        
        LocalDate endDate,
        
        ProjectStatus projectStatus,
        
        Map<String, ProjectMemberDTO> members,
        
        Set<String> taskIds
) {}

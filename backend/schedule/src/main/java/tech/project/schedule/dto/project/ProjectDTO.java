package tech.project.schedule.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tech.project.schedule.dto.project.ProjectMemberDTO;
import tech.project.schedule.model.enums.ProjectStatus;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ProjectDTO(
        UUID id,
        @NotBlank(message = "Project name is required")
        String name,
        String description,
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        LocalDate endDate,
        ProjectStatus projectStatus,
        Map<String, ProjectMemberDTO> members,  // Zmiana z UUID na String
        Set<String> taskIds  // Zmiana z UUID na String
) {}
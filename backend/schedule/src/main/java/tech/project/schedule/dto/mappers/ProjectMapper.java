package tech.project.schedule.dto.mappers;

import tech.project.schedule.dto.project.ProjectDTO;
import tech.project.schedule.dto.project.ProjectMemberDTO;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.project.ProjectMember;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class that provides conversion methods between Project-related entities and DTOs.
 * Handles the mapping of data between the persistence layer (entities) and the presentation layer (DTOs)
 * for projects, project members, and related data.
 */
public class ProjectMapper {

     /**
     * Converts a ProjectDTO to a Project entity.
     * 
     * @param dto The ProjectDTO to convert
     * @return A new Project entity with properties set from the DTO
     */
    public static Project dtoToProject(ProjectDTO dto) {
        Project project = new Project();

        
        project.setName(dto.name());
        project.setDescription(dto.description());
        project.setStartDate(dto.startDate());
        project.setEndDate(dto.endDate());
        project.setProjectStatus(dto.projectStatus());
        
        return project;
    }

    /**
     * Converts a Project entity to a ProjectDTO.
     * Also handles conversion of associated members and task IDs.
     * 
     * @param project The Project entity to convert
     * @return A new ProjectDTO containing data from the Project entity
     */
    public static ProjectDTO projectToDTO(Project project) {
        Map<String, ProjectMemberDTO> memberDTOs = new HashMap<>();
        
        if (project.getMembers() != null) {
            memberDTOs = project.getMembers().entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().toString(),
                            entry -> memberToDTO(entry.getValue())
                    ));
        }
        
        Set<String> taskIds = new HashSet<>();
        if (project.getTasks() != null) {
            taskIds = project.getTasks().stream()
                    .map(task -> task.getId().toString())
                    .collect(Collectors.toSet());
        }
        
        return new ProjectDTO(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStartDate(),
                project.getEndDate(),
                project.getProjectStatus(),
                memberDTOs,
                taskIds
        );
    }

      /**
     * Converts a ProjectMember entity to a ProjectMemberDTO.
     * 
     * @param member The ProjectMember entity to convert
     * @return A new ProjectMemberDTO containing data from the ProjectMember entity
     */
    public static ProjectMemberDTO memberToDTO(ProjectMember member) {
        return new ProjectMemberDTO(
                member.getId(),
                member.getProject().getId(),
                member.getUser().getId(),
                member.getUser().getName(),
                member.getRole()
        );
    }
}

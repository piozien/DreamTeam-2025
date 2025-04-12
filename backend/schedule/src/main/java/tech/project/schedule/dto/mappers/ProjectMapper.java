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

public class ProjectMapper {

    public static Project dtoToProject(ProjectDTO dto) {
        Project project = new Project();

        
        project.setName(dto.name());
        project.setDescription(dto.description());
        project.setStartDate(dto.startDate());
        
        return project;
    }

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
    
    public static ProjectMemberDTO memberToDTO(ProjectMember member) {
        return new ProjectMemberDTO(
                member.getId(),
                member.getProject().getId(),
                member.getUser().getId(),
                member.getRole()
        );
    }
}

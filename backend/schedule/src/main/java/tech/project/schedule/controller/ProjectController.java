package tech.project.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.project.schedule.dto.project.AddProjectMemberDTO;
import tech.project.schedule.dto.project.ProjectDTO;
import tech.project.schedule.dto.project.ProjectMemberDTO;
import tech.project.schedule.dto.project.UpdateProjectMemberRoleDTO;
import tech.project.schedule.dto.mappers.ProjectMapper;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.ProjectService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(
            @Valid @RequestBody ProjectDTO projectDTO,
            @RequestParam UUID userId) {
        
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        Project project = ProjectMapper.dtoToProject(projectDTO);
        
        Project createdProject = projectService.createProject(project, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMapper.projectToDTO(createdProject));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> getProject(
            @PathVariable UUID projectId, 
            @RequestParam UUID userId) {
        
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        Project project = projectService.getProjectById(projectId, currentUser);
        
        return ResponseEntity.ok(ProjectMapper.projectToDTO(project));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable UUID projectId, 
            @Valid @RequestBody ProjectDTO projectDTO, 
            @RequestParam UUID userId
    ) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        Project project = ProjectMapper.dtoToProject(projectDTO);
        
        Project updatedProject = projectService.updateProject(projectId, project, currentUser);
        
        return ResponseEntity.ok(ProjectMapper.projectToDTO(updatedProject));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID projectId, 
            @RequestParam UUID userId) {
        
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        projectService.deleteProject(projectId, currentUser);
        
        return ResponseEntity.noContent().build();
    }

    
    @PostMapping("/{projectId}/members")
    public ResponseEntity<ProjectMemberDTO> addMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectMemberDTO memberDTO
    ) {
        UUID userId = memberDTO.userId();
        ProjectUserRole role = memberDTO.role();
        
        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        ProjectMember addedMember = projectService.addMemberToProject(projectId, userToAdd, role);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMapper.memberToDTO(addedMember));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @RequestParam UUID currentUserId
    ) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        projectService.removeMemberFromProject(projectId, userId, currentUser);
        
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{projectId}/members/{userId}")
    public ResponseEntity<ProjectMemberDTO> updateMemberRole(
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProjectMemberRoleDTO roleDTO,
            @RequestParam UUID currentUserId
    ) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        ProjectUserRole newRole = roleDTO.role();
        
        ProjectMember updatedMember = projectService.updateMemberRole(projectId, userId, newRole, currentUser);
        
        return ResponseEntity.ok(ProjectMapper.memberToDTO(updatedMember));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<Map<String, ProjectMemberDTO>> getProjectMembers(
            @PathVariable UUID projectId,
            @RequestParam UUID userId
    ) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        Map<UUID, ProjectMember> members = projectService.getProjectMembers(projectId, currentUser);
        
        Map<String, ProjectMemberDTO> memberDTOs = members.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> ProjectMapper.memberToDTO(entry.getValue())
                ));
        
        return ResponseEntity.ok(memberDTOs);
    }
    
    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getUserProjects(
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        List<Project> projects = projectService.getUserProjects(user);
        List<ProjectDTO> projectDTOs = projects.stream()
                .map(ProjectMapper::projectToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(projectDTOs);
    }
}

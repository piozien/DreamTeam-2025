package tech.project.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.project.schedule.dto.project.AddProjectMemberDTO;
import tech.project.schedule.dto.project.ProjectDTO;
import tech.project.schedule.dto.project.ProjectMemberDTO;
import tech.project.schedule.dto.project.ProjectUpdateDTO;
import tech.project.schedule.dto.project.UpdateProjectMemberRoleDTO;
import tech.project.schedule.dto.mappers.ProjectMapper;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.ProjectService;
import tech.project.schedule.utils.UserUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for managing project-related operations.
 * Provides endpoints for creating, retrieving, updating, and deleting projects,
 * as well as managing project members and their roles.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

     /**
     * Creates a new project with the current user as the owner.
     *
     * @param projectDTO Data transfer object containing project details
     * @param userId ID of the user creating the project
     * @return ResponseEntity containing the created project as DTO with HTTP status 201 (CREATED)
     * @throws ApiException if the user is not found
     */
    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(
            @Valid @RequestBody ProjectDTO projectDTO,
            @RequestParam UUID userId) {
        
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(currentUser);
        
        Project project = ProjectMapper.dtoToProject(projectDTO);
        
        Project createdProject = projectService.createProject(project, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMapper.projectToDTO(createdProject));
    }
    

    /**
     * Retrieves a project by its ID if the user has access to it.
     *
     * @param projectId ID of the project to retrieve
     * @param userId ID of the user requesting the project
     * @return ResponseEntity containing the project as DTO
     * @throws ApiException if the user or project is not found, or if user lacks access
    */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> getProject(
            @PathVariable UUID projectId, 
            @RequestParam UUID userId) {
        
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(currentUser);
        
        Project project = projectService.getProjectById(projectId, currentUser);
        
        return ResponseEntity.ok(ProjectMapper.projectToDTO(project));
    }

      /**
     * Updates an existing project if the user has appropriate permissions.
     *
     * @param projectId ID of the project to update
     * @param projectDTO Data transfer object containing updated project details
     * @param userId ID of the user requesting the update
     * @return ResponseEntity containing the updated project as DTO
     * @throws ApiException if the user or project is not found, or if user lacks permissions
     */
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable UUID projectId, 
            @Valid @RequestBody ProjectDTO projectDTO, 
            @RequestParam UUID userId
    ) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(currentUser);
        
        Project project = ProjectMapper.dtoToProject(projectDTO);
        
        Project updatedProject = projectService.updateProject(projectId, project, currentUser);
        
        return ResponseEntity.ok(ProjectMapper.projectToDTO(updatedProject));
    }

     /**
     * Deletes a project if the user has appropriate permissions.
     *
     * @param projectId ID of the project to delete
     * @param userId ID of the user requesting the deletion
     * @return ResponseEntity with HTTP status 204 (NO CONTENT) on successful deletion
     * @throws ApiException if the user or project is not found, or if user lacks permissions
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID projectId, 
            @RequestParam UUID userId) {
        
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(currentUser);
        
        projectService.deleteProject(projectId, currentUser);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a new member to a project with a specified role.
     *
     * @param projectId ID of the project to add member to
     * @param memberDTO Data transfer object containing user ID and role
     * @param currentUserId ID of the user performing the action
     * @return ResponseEntity containing the added project member as DTO with HTTP status 201 (CREATED)
     * @throws ApiException if users are not found, project doesn't exist, or current user lacks permissions
     */
    @PostMapping("/{projectId}/members")
    public ResponseEntity<ProjectMemberDTO> addMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectMemberDTO memberDTO,
            @RequestParam UUID currentUserId

    ) {
        UUID userId = memberDTO.userId();
        ProjectUserRole role = memberDTO.role();
        
        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        User principal = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        ProjectMember addedMember = projectService.addMemberToProject(projectId, userToAdd, role, principal);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMapper.memberToDTO(addedMember));
    }
    

    /**
     * Removes a member from a project.
     *
     * @param projectId ID of the project
     * @param userId ID of the user to be removed
     * @param currentUserId ID of the user performing the action
     * @return ResponseEntity with HTTP status 204 (NO CONTENT) on successful removal
     * @throws ApiException if users are not found, project doesn't exist, or current user lacks permissions
     */
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
    
     /**
     * Updates the role of an existing project member.
     *
     * @param projectId ID of the project
     * @param userId ID of the user whose role is being updated
     * @param roleDTO Data transfer object containing the new role
     * @param currentUserId ID of the user performing the action
     * @return ResponseEntity containing the updated project member as DTO
     * @throws ApiException if users are not found, project doesn't exist, or current user lacks permissions
     */
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
    
     /**
     * Retrieves all members of a specific project.
     *
     * @param projectId ID of the project
     * @param userId ID of the user requesting the member list
     * @return ResponseEntity containing a map of user IDs to project member DTOs
     * @throws ApiException if user is not found, project doesn't exist, or user lacks access
     */
    @GetMapping("/{projectId}/members")
    public ResponseEntity<Map<String, ProjectMemberDTO>> getProjectMembers(
            @PathVariable UUID projectId,
            @RequestParam UUID userId
    ) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(currentUser);
        
        Map<UUID, ProjectMember> members = projectService.getProjectMembers(projectId, currentUser);
        
        Map<String, ProjectMemberDTO> memberDTOs = members.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> ProjectMapper.memberToDTO(entry.getValue())
                ));
        
        return ResponseEntity.ok(memberDTOs);
    }

       /**
     * Retrieves all projects that the specified user is a member of.
     *
     * @param userId ID of the user whose projects are being retrieved
     * @return ResponseEntity containing a list of project DTOs
     * @throws ApiException if the user is not found
     */
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

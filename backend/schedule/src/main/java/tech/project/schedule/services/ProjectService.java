package tech.project.schedule.services;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.ProjectStatus;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.enums.TaskStatus;
import tech.project.schedule.model.project.Project;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.ProjectRepository;
import tech.project.schedule.services.utils.GetProjectRole;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service class for managing project-related operations.
 * Provides business logic for project creation, management, and access control,
 * enforcing rules regarding project status, member roles, and operation permissions.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;

    /**
     * Creates a new project with the current user as the Project Manager.
     * Sets project status based on start date and assigns the creator as PM.
     *
     * @param project The project entity to be created
     * @param user The user creating the project
     * @return The newly created project
     * @throws ApiException if a project with the same name exists or required fields are missing
     */
    @Transactional
    public Project createProject(Project project, User user) {
        if (projectRepository.existsByName(project.getName())) {
            throw new ApiException("Project with name " + project.getName() + " already exists.", HttpStatus.CONFLICT);
        }
        if (project.getStartDate() == null) {
            throw new ApiException("Start date is required", HttpStatus.BAD_REQUEST);
        }
        
        LocalDate today = LocalDate.now();
        if (project.getStartDate().isAfter(today)) {
            project.setProjectStatus(ProjectStatus.PLANNED);
        } else {
            project.setProjectStatus(ProjectStatus.IN_PROGRESS);
        }
        
        ProjectMember setPM = new ProjectMember(user, ProjectUserRole.PM);
        Project newProject = projectRepository.save(project);
        setPM.setProject(newProject);

        newProject.addMember(user.getId(), setPM);

        return projectRepository.save(newProject);
    }

    /**
     * Updates an existing project's details.
     * Only Project Managers can update projects, and completion status is only
     * allowed when all tasks are finished.
     *
     * @param projectId The ID of the project to update
     * @param updatedProject Project entity containing the updated values
     * @param user The user attempting the update operation
     * @return The updated project
     * @throws ApiException if project not found, user lacks permission, or business rules are violated
     */
    @Transactional
    public Project updateProject(UUID projectId, Project updatedProject, User user) {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));


        boolean isPM = GetProjectRole.getProjectRole(user, existingProject) == ProjectUserRole.PM;

        if (!isPM) {
            throw new ApiException("You cannot edit this project", HttpStatus.FORBIDDEN);
        }
        if (updatedProject.getName() != null) {
            if (!updatedProject.getName().equals(existingProject.getName())
                    && projectRepository.existsByName(updatedProject.getName())) {
                throw new ApiException("Project with name " + updatedProject.getName() + " already exists.", HttpStatus.CONFLICT);
            }
            existingProject.setName(updatedProject.getName());
        }
        if (updatedProject.getDescription() != null) {
            existingProject.setDescription(updatedProject.getDescription());
        }
        if (updatedProject.getStartDate() != null) {
            existingProject.setStartDate(updatedProject.getStartDate());
        }
        if (updatedProject.getEndDate() != null) {
            existingProject.setEndDate(updatedProject.getEndDate());
        }

        if (updatedProject.getProjectStatus() != null) {
            if (ProjectStatus.COMPLETED.equals(updatedProject.getProjectStatus())) {
                if (existingProject.getTasks() != null && !existingProject.getTasks().isEmpty() && 
                    !existingProject.getTasks().stream().allMatch(task -> task.getStatus().equals(TaskStatus.FINISHED))) {
                    throw new ApiException("Project has unfinished tasks", HttpStatus.CONFLICT);
                }
            }
            existingProject.setProjectStatus(updatedProject.getProjectStatus());
        }

        return projectRepository.save(existingProject);
    }

    /**
     * Deletes a project.
     * Only Project Managers can delete projects.
     *
     * @param projectId The ID of the project to delete
     * @param user The user attempting the delete operation
     * @throws ApiException if project not found or user lacks permission
     */
    @Transactional
    public void deleteProject(UUID projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));


        boolean isPM = GetProjectRole.getProjectRole(user, project) == ProjectUserRole.PM;


        if (!isPM) {
            throw new ApiException("You cannot delete this project", HttpStatus.FORBIDDEN);
        }
        
        projectRepository.deleteById(projectId);
    }

    /**
     * Retrieves a project by its ID, with access control.
     * Only members of the project can view its details.
     *
     * @param projectId The ID of the project to retrieve
     * @param user The user attempting to access the project
     * @return The requested project
     * @throws ApiException if project not found or user lacks permission
     */
    public Project getProjectById(UUID projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));


        ProjectUserRole userRole = GetProjectRole.getProjectRole(user, project);
        boolean isProjectMember = userRole != null; 
        
        if (!isProjectMember) {
            throw new ApiException("You are not a member of this project", HttpStatus.FORBIDDEN);
        }
        
        return project;
    }

    /**
     * Adds a new member to a project.
     * Only Project Managers can add members, and users can't be added twice.
     *
     * @param projectId The ID of the project
     * @param user The user to add to the project
     * @param role The role to assign to the new member
     * @param principal The user performing the operation
     * @return The created project member entity
     * @throws ApiException if project not found, user lacks permission, or user is already a member
     */
    @Transactional
    public ProjectMember addMemberToProject(UUID projectId, User user, ProjectUserRole role, User principal) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));
                

        boolean isPM = GetProjectRole.getProjectRole(principal, project) == ProjectUserRole.PM;

        if (!isPM) {
            throw new ApiException("You cannot add members to this project", HttpStatus.FORBIDDEN);
        }
        
        if (project.getMembers().containsKey(user.getId())) {
            throw new ApiException("User is already a member of this project", HttpStatus.CONFLICT);
        }
        
        ProjectMember newMember = new ProjectMember(user, role);
        newMember.setProject(project);
        project.addMember(user.getId(), newMember);
        
        projectRepository.save(project);
        return newMember;
    }

    /**
     * Removes a member from a project.
     * Only Project Managers can remove members, and the last PM cannot be removed.
     *
     * @param projectId The ID of the project
     * @param userId The ID of the user to remove
     * @param currentUser The user performing the operation
     * @throws ApiException if project not found, user lacks permission, user not found, or removing the last PM
     */
    @Transactional
    public void removeMemberFromProject(UUID projectId, UUID userId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));

        boolean isPM = GetProjectRole.getProjectRole(currentUser, project) == ProjectUserRole.PM;
        
        if (!isPM) {
            throw new ApiException("You cannot remove members from this project", HttpStatus.FORBIDDEN);
        }
        
        if (!project.getMembers().containsKey(userId)) {
            throw new ApiException("User is not a member of this project", HttpStatus.NOT_FOUND);
        }
        
        if (ProjectUserRole.PM.equals(project.getMembers().get(userId).getRole())) {
            long pmCount = project.getMembers().values().stream()
                    .filter(member -> ProjectUserRole.PM.equals(member.getRole()))
                    .count();
            
            if (pmCount <= 1) {
                throw new ApiException("Cannot remove the last Project Manager", HttpStatus.CONFLICT);
            }
        }
        
        project.getMembers().remove(userId);
        projectRepository.save(project);
    }

    /**
     * Updates a project member's role.
     * Only Project Managers can update roles, and the last PM cannot be demoted.
     *
     * @param projectId The ID of the project
     * @param userId The ID of the user whose role is being updated
     * @param newRole The new role to assign
     * @param currentUser The user performing the operation
     * @return The updated project member entity
     * @throws ApiException if project not found, user lacks permission, user not found, or demoting the last PM
     */
    @Transactional
    public ProjectMember updateMemberRole(UUID projectId, UUID userId, ProjectUserRole newRole, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));
        boolean isPM = GetProjectRole.getProjectRole(currentUser, project) == ProjectUserRole.PM;
        
        if (!isPM) {
            throw new ApiException("You cannot update member roles in this project", HttpStatus.FORBIDDEN);
        }
        
        ProjectMember member = project.getMembers().get(userId);
        if (member == null) {
            throw new ApiException("User is not a member of this project", HttpStatus.NOT_FOUND);
        }
        
        if (ProjectUserRole.PM.equals(member.getRole()) && !ProjectUserRole.PM.equals(newRole)) {
            long pmCount = project.getMembers().values().stream()
                    .filter(m -> ProjectUserRole.PM.equals(m.getRole()))
                    .count();
            
            if (pmCount <= 1) {
                throw new ApiException("Cannot downgrade the last Project Manager", HttpStatus.CONFLICT);
            }
        }
        
        member.setRole(newRole);
        projectRepository.save(project);
        return member;
    }

    /**
     * Retrieves all members of a project.
     * Only project members, PMs, and admins can view the member list.
     *
     * @param projectId The ID of the project
     * @param currentUser The user attempting to access the member list
     * @return Map of user IDs to project member entities
     * @throws ApiException if project not found or user lacks permission
     */
    public Map<UUID, ProjectMember> getProjectMembers(UUID projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));
                
        ProjectUserRole userRole = GetProjectRole.getProjectRole(currentUser, project);
        boolean isMember = userRole == ProjectUserRole.MEMBER;
        boolean isPM = userRole == ProjectUserRole.PM;
        boolean isAdmin = currentUser.getGlobalRole() == GlobalRole.ADMIN;
        
        if (!isAdmin && !isMember && !isPM) {
            throw new ApiException("You are not a member of this project", HttpStatus.FORBIDDEN);
        }
        
        return project.getMembers();
    }
    
    /**
     * Retrieves all projects associated with a user.
     * Admins can see all projects, while regular users see only their own.
     *
     * @param user The user whose projects are being retrieved
     * @return List of projects associated with the user
     */
    public List<Project> getUserProjects(User user) {
        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
        
        if (isAdmin) {
            return projectRepository.findAll();
        }
        
        return user.getProjectMembers().stream()
                .map(ProjectMember::getProject)
                .distinct()
                .toList();
    }
}

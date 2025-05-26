package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.enums.ProjectStatus;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.enums.TaskStatus;
import tech.project.schedule.model.project.Project;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.ProjectRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.repositories.TaskAssigneeRepository;
import tech.project.schedule.services.utils.GetProjectRole;
import tech.project.schedule.services.utils.NotificationHelper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing projects and their members.
 * Handles project lifecycle operations and member access management with
 * appropriate permission checks and business rule validations.
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final NotificationHelper notificationHelper;
    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;

    /**
     * Creates a new project with the current user as Project Manager.
     * Validates the project details and sets the current user as the PM.
     * 
     * @param project The project entity to create
     * @param user The user creating the project (will become PM)
     * @return The created and persisted project entity
     * @throws ApiException if project name already exists or invalid dates
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
        Project savedProject = projectRepository.save(newProject);
        
        // Powiadom twórcę projektu
        notificationHelper.notifyProjectMember(
            user,
            NotificationStatus.PROJECT_CREATED,
            savedProject.getName()
        );
        
        return savedProject;
    }

    /**
     * Updates an existing project's details.
     * Only Project Managers can update projects, and completion requires all tasks to be finished.
     * 
     * @param projectId ID of the project to update
     * @param updatedProject Project entity containing the updated fields
     * @param user The user performing the update
     * @return The updated project entity
     * @throws ApiException if project not found, user lacks permission, or business rules violated
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

        Project savedProject = projectRepository.save(existingProject);
        
        // All project members get notified about the update
        existingProject.getMembers().values().forEach(member -> {
            if (!member.getUser().getId().equals(user.getId())) {
                notificationHelper.notifyProjectMember(
                    member.getUser(),
                    NotificationStatus.PROJECT_UPDATED,
                    existingProject.getName()
                );
            }
        });
        
        return savedProject;
    }

    /**
     * Deletes a project from the system.
     * Only Project Managers can delete projects.
     * 
     * @param projectId ID of the project to delete
     * @param user The user attempting to delete the project
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
        
        // Save project name and members before deletion
        String projectName = project.getName();
        List<User> members = project.getMembers().values().stream()
            .map(ProjectMember::getUser)
            .collect(Collectors.toList());
        
        projectRepository.deleteById(projectId);
        
        // All project members get notified about the project deletion.
        members.forEach(member -> {
            if (!member.getId().equals(user.getId())) {
                notificationHelper.notifyProjectMember(
                    member,
                    NotificationStatus.PROJECT_DELETED,
                    projectName
                );
            }
        });
    }

    /**
     * Retrieves a project by its ID.
     * Only project members can view a project.
     * 
     * @param projectId ID of the project to retrieve
     * @param user The user requesting the project
     * @return The requested project entity
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
     * Adds a user to a project with the specified role.
     * Only Project Managers can add members to projects.
     * 
     * @param projectId ID of the project to add the member to
     * @param user The user to add as a member
     * @param role The role to assign to the new member
     * @param principal The user performing the addition
     * @return The created ProjectMember entity
     * @throws ApiException if project not found, user lacks permission, or user already a member
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
        ProjectMember savedMember = project.getMembers().get(user.getId());

        // The user getting added gets notified
        notificationHelper.notifyProjectMember(
            user,
            NotificationStatus.PROJECT_MEMBER_ADDED,
            project.getName()
        );
        
        // The admin gets notified that the project member has been added successfully
        notificationHelper.notifyUser(
            principal,
            NotificationStatus.PROJECT_MEMBER_ADDED,
            "Dodano użytkownika " + user.getUsername() + " do projektu " + project.getName()
        );

        return savedMember;
    }

    /**
     * Removes a member from a project.
     * Only Project Managers can remove members, and the last PM cannot be removed.
     * 
     * @param projectId ID of the project to remove the member from
     * @param userId ID of the user to remove
     * @param currentUser The user performing the removal
     * @throws ApiException if project not found, user lacks permission, or removing last PM
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

        User removedUser = project.getMembers().get(userId).getUser();
        String projectName = project.getName();
        
        // Remove all task assignments for this user in the project
        Set<Task> projectTasks = project.getTasks();
        if (projectTasks != null) {
            for (Task task : projectTasks) {
                Set<TaskAssignee> assignees = task.getAssignees();
                if (assignees != null) {
                    // Remove assignments using repository
                    List<TaskAssignee> assignmentsToRemove = assignees.stream()
                        .filter(assignee -> assignee.getUser().getId().equals(userId))
                        .collect(Collectors.toList());
                    
                    taskAssigneeRepository.deleteAll(assignmentsToRemove);
                    
                    // Update task assignees collection
                    assignees.removeAll(assignmentsToRemove);
                    
                    // Save task to persist changes
                    taskRepository.save(task);
                }
            }
        }
        
        project.getMembers().remove(userId);
        projectRepository.save(project);
        
        notificationHelper.notifyUser(
            removedUser,
            NotificationStatus.PROJECT_UPDATED, 
            "Zostałeś usunięty z projektu " + projectName
        );
    }

    /**
     * Updates a project member's role.
     * Only Project Managers can change roles, and the last PM cannot be downgraded.
     * 
     * @param projectId ID of the project
     * @param userId ID of the member to update
     * @param newRole The new role to assign
     * @param currentUser The user performing the update
     * @return The updated ProjectMember entity
     * @throws ApiException if project not found, user lacks permission, or downgrading last PM
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
        
        // Zapisz poprzednią rolę dla wiadomości
        ProjectUserRole oldRole = member.getRole();
        
        member.setRole(newRole);
        projectRepository.save(project);
        
        // Powiadom użytkownika o zmianie roli
        notificationHelper.notifyUser(
            member.getUser(),
            NotificationStatus.PROJECT_UPDATED,
            "Twoja rola w projekcie " + project.getName() + " została zmieniona z " + oldRole + " na " + newRole
        );
        
        return member;
    }

    /**
     * Retrieves all members of a project.
     * Only project members, PMs, and admins can view project members.
     * 
     * @param projectId ID of the project
     * @param currentUser The user requesting the member list
     * @return Map of user IDs to ProjectMember entities
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
     * Gets all projects the user is a member of.
     * Admins can see all projects in the system.
     * 
     * @param user The user whose projects to retrieve
     * @return List of projects the user is a member of
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

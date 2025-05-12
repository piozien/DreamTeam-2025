package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.*;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.project.Project;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.ProjectRepository;
import tech.project.schedule.services.utils.GetProjectRole;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ProjectService {
    private final NotificationService notificationService;
    private final ProjectRepository projectRepository;

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

        notificationService.sendNotificationToUser(
                user,
                NotificationStatus.PROJECT_CREATED,
                "You have created a project by the name: "+ project.getName()+" successfully."
        );
        return savedProject;
    }

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
        notificationService.sendNotificationToUser(
                savedMember.getUser(),
                NotificationStatus.PROJECT_MEMBER_ADDED,
                "You have been added to the project: "+ project.getName()
        );
        notificationService.sendNotificationToUser(
                principal,
                NotificationStatus.PROJECT_MEMBER_ADDED,
                "You added " + user.getUsername() + " to the project: " + project.getName()
        );

        return savedMember;
    }
    
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

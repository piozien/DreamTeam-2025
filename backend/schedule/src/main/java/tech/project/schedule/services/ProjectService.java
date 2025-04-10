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

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;

    @Transactional
    public Project createProject(Project project, User user) {
        if (projectRepository.existsByName(project.getName())) {
            throw new ApiException("Project with name " + project.getName() + " already exists.", HttpStatus.CONFLICT);
        }
        if (project.getStartDate() == null) {
            throw new ApiException("Start date is required", HttpStatus.BAD_REQUEST);
        }

        if (project.getMembers() == null) {
            project.setMembers(new HashMap<>());
        }
        if (project.getTasks() == null) {
            project.setTasks(new HashSet<>());
        }

        if (project.getProjectStatus() == null) {
            project.setProjectStatus(ProjectStatus.PLANNED);
        }

        ProjectMember setPM = new ProjectMember(user, ProjectUserRole.PM);
        Project newProject = projectRepository.save(project);
        setPM.setProject(newProject);

        newProject.addMember(user.getId(), setPM);

        return projectRepository.save(newProject);
    }

    @Transactional
    public Project updateProject(UUID projectId, Project updatedProject, User user) {
        Project existingProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));

        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
        boolean isPM = existingProject.getMembers().containsKey(user.getId()) &&
                ProjectUserRole.PM.equals(existingProject.getMembers().get(user.getId()).getRole());

        if (!isAdmin && !isPM) {
            throw new ApiException("You cannot edit this project", HttpStatus.FORBIDDEN);
        }
        if (updatedProject.getName() != null) {
            if (!updatedProject.getName().equals(existingProject.getName())
                    && projectRepository.existsByName(updatedProject.getName())) {
                throw new ApiException("Project with name " + updatedProject.getName() + " already exists.", HttpStatus.CONFLICT);
            }
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
            if(!updatedProject.getTasks().stream().allMatch(task -> task.getStatus().equals(TaskStatus.FINISHED))){
                throw new ApiException("Project has unfinished tasks", HttpStatus.CONFLICT);
            }
            existingProject.setProjectStatus(updatedProject.getProjectStatus());
        }

        return projectRepository.save(existingProject);
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ApiException("Project not found", HttpStatus.NOT_FOUND);
        }
        projectRepository.deleteById(projectId);
    }
}

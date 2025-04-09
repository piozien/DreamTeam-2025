package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.dto.project.ProjectDTO;
import tech.project.schedule.dto.project.ProjectMemberDTO;
import tech.project.schedule.exception.ProjectNotFoundException;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.repositories.ProjectRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;

    @Transactional
    public Project createProject(ProjectDTO projectDTO) {
        Project project = new Project();
        project.setName(projectDTO.name());
        project.setDescription(projectDTO.description());
        project.setStartDate(projectDTO.startDate());
        project.setEndDate(projectDTO.endDate());
        project.setProjectStatus(projectDTO.projectStatus());

        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Project getProjectById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with id: " + id));
    }

    @Transactional
    public Project updateProject(UUID id, ProjectDTO projectDTO) {
        Project existingProject = getProjectById(id);

        existingProject.setName(projectDTO.name());
        existingProject.setDescription(projectDTO.description());
        existingProject.setStartDate(projectDTO.startDate());
        existingProject.setEndDate(projectDTO.endDate());
        existingProject.setProjectStatus(projectDTO.projectStatus());

        return projectRepository.save(existingProject);
    }

    @Transactional
    public void deleteProject(UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new ProjectNotFoundException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }

    @Transactional
    public Project addProjectMember(UUID projectId, ProjectMemberDTO memberDTO) {
        Project project = getProjectById(projectId);

        ProjectMember member = new ProjectMember();
        member.setId(memberDTO.id());
        member.setRole(memberDTO.role());

        project.addMember(memberDTO.userId(), member);
        return projectRepository.save(project);
    }

    @Transactional
    public Project removeProjectMember(UUID projectId, UUID userId) {
        Project project = getProjectById(projectId);
        project.getMembers().remove(userId);
        return projectRepository.save(project);
    }
}
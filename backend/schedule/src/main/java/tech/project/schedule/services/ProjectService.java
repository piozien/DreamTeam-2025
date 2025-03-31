package tech.project.schedule.services;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.project.Project;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.ProjectRepository;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;

    @Transactional
    public Project createProject(Project project, User user) {
        if(!projectRepository.existsByName(project.getName())) {
            throw new ApiException("Project with name " + project.getName() + " already exists.", HttpStatus.CONFLICT);
        }
        project.setStartDate(LocalDate.now());
        //user.getId(), new ProjectMember(user, ProjectUserRole.PM)
//        project.setMembers();
        return projectRepository.save(project);
    }






}

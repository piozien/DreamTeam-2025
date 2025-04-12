package tech.project.schedule.dto.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import tech.project.schedule.dto.task.TaskAssigneeDTO;
import tech.project.schedule.dto.task.TaskCommentDTO;
import tech.project.schedule.dto.task.TaskDTO;
import tech.project.schedule.dto.task.TaskFileDTO;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.task.*;
import tech.project.schedule.model.user.User;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
//import tech.project.schedule.repositories.ProjectRepository;

@RequiredArgsConstructor
public class TaskMapper {
//    private final ProjectRepository projectRepository;
    //todo: do something with all of those sets also couldn't get project from dto because of projectRepository
    public static Task dtoToTask(TaskDTO dto) {
        Task task = new Task();


        task.setName(dto.name());
        task.setDescription(dto.description());
        task.setStartDate(dto.startDate());
        task.setPriority(dto.priority());
        task.setStatus(dto.status());
        return task;
    }




    public static TaskDTO taskToDTO(Task task) {
        Set<UUID> assigneeIds = new HashSet<>();
        if(task.getAssignees() != null) {
            assigneeIds = task.getAssignees().stream().map(TaskAssignee::getId)
                    .collect(Collectors.toSet());
        }
        Set<TaskCommentDTO> commentDtos = new HashSet<>();
        if(task.getComments() != null) {
            commentDtos = task.getComments().stream().map(TaskComment::mapToTaskCommentDTO)
                    .collect(Collectors.toSet());
        }
        Set<TaskFileDTO> taskFileDtos = new HashSet<>();
        if(task.getFiles() != null) {
            taskFileDtos = task.getFiles().stream().map(TaskFile::mapToTaskFileDTO)
                    .collect(Collectors.toSet());
        }
        Set<UUID> dependencyIds = new HashSet<>();
        if(task.getDependencies() != null) {
            dependencyIds = task.getDependencies().stream().map(TaskDependency::getId)
                    .collect(Collectors.toSet());
        }
        return new TaskDTO(
                task.getProject().getId(),
                task.getName(),
                task.getDescription(),
                task.getStartDate(),
                task.getPriority(),
                task.getStatus(),
                assigneeIds,
                commentDtos,
                taskFileDtos,
                dependencyIds
        );
    }
    // ToDo: Finish assignee to DTO method
    public static TaskAssigneeDTO assigneeToDTO(TaskAssignee addedAssignee) {
        return null;
    }
}

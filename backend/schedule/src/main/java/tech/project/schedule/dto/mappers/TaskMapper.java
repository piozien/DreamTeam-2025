package tech.project.schedule.dto.mappers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

//import tech.project.schedule.repositories.TaskRepository;
//import tech.project.schedule.repositories.UserRepository;
//import tech.project.schedule.repositories.ProjectRepository;


@RequiredArgsConstructor
public class TaskMapper {

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

    public static TaskAssigneeDTO assigneeToDTO(TaskAssignee assignee) {
        return new TaskAssigneeDTO(
          assignee.getId(),
          assignee.getTask().getId(),
          assignee.getUser().getId()
        );
    }

    // ToDo: Static methods are a pain
    public static TaskComment dtoToComment(TaskCommentDTO taskCommentDTO) {
        TaskComment comment = new TaskComment();

//        comment.setTask();
//        comment.setUser();
        comment.setComment(taskCommentDTO.comment());
        comment.setCreatedAt(taskCommentDTO.createdAt());
        return comment;
    }

    public static TaskCommentDTO commentToDTO(TaskComment createdComment) {
        return new TaskCommentDTO(
                createdComment.getId(),
                createdComment.getTask().getId(),
                createdComment.getUser().getId(),
                createdComment.getComment(),
                createdComment.getCreatedAt()
        );
    }

    public static TaskFile dtoToFile(@Valid TaskFileDTO taskFileDTO) {
        return null;
    }

    public static TaskFileDTO fileToDTO(TaskFile createdFile) {
        return null;
    }
}

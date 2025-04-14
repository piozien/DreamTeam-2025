package tech.project.schedule.dto.mappers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tech.project.schedule.dto.task.TaskAssigneeDTO;
import tech.project.schedule.dto.task.TaskCommentDTO;
import tech.project.schedule.dto.task.TaskDTO;
import tech.project.schedule.dto.task.TaskFileDTO;
import tech.project.schedule.dto.task.TaskRequestDTO;
import tech.project.schedule.dto.task.TaskUpdateDTO;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.task.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@RequiredArgsConstructor
public class TaskMapper {

    public static Task requestDtoToTask(TaskRequestDTO dto) {
        Task task = new Task();
        
        Project project = new Project();
        project.setId(dto.projectId());
        task.setProject(project);
        
        task.setName(dto.name());
        task.setDescription(dto.description());
        task.setStartDate(dto.startDate());
        task.setEndDate(dto.endDate());
        task.setPriority(dto.priority());
        task.setStatus(dto.status());
        return task;
    }
    
    public static Task updateDtoToTask(TaskUpdateDTO dto) {
        Task task = new Task();
        
        if (dto.id() != null) {
            task.setId(dto.id());
        }
        task.setName(dto.name());
        task.setDescription(dto.description());
        task.setStartDate(dto.startDate());
        task.setEndDate(dto.endDate());
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
                task.getId(),
                task.getProject().getId(),
                task.getName(),
                task.getDescription(),
                task.getStartDate(),
                task.getEndDate(),
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

    public static TaskComment dtoToComment(TaskCommentDTO taskCommentDTO) {
        TaskComment comment = new TaskComment();

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
        TaskFile file = new TaskFile();
        if (taskFileDTO.id() != null) {
            file.setId(taskFileDTO.id());
        }
        file.setFilePath(taskFileDTO.filePath());
        return file;
    }

    public static TaskFileDTO fileToDTO(TaskFile createdFile) {
        return new TaskFileDTO(
            createdFile.getId(),
            createdFile.getTask().getId(),
            createdFile.getUploadedBy().getId(),
            createdFile.getFilePath(),
            createdFile.getUploadedAt()
        );
    }
}

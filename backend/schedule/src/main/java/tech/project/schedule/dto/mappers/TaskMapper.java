package tech.project.schedule.dto.mappers;

import lombok.RequiredArgsConstructor;
import tech.project.schedule.dto.task.TaskAssigneeDTO;
import tech.project.schedule.dto.task.TaskCommentDTO;
import tech.project.schedule.dto.task.TaskDTO;
import tech.project.schedule.dto.task.TaskRequestDTO;
import tech.project.schedule.dto.task.TaskUpdateDTO;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.task.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class that provides mapping methods between Task-related entities and DTOs.
 * Handles the conversion of data between domain models and data transfer objects
 * for tasks and their associated elements like assignees and comments.
 */
@RequiredArgsConstructor
public class TaskMapper {

     /**
     * Converts a TaskRequestDTO to a Task entity.
     * 
     * @param dto The TaskRequestDTO containing data for creating a new task
     * @return A new Task entity initialized with data from the DTO
     */
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

      /**
     * Converts a TaskUpdateDTO to a Task entity.
     * 
     * @param dto The TaskUpdateDTO containing the updated task information
     * @return A Task entity with properties set from the update DTO
     */
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



     /**
     * Converts a Task entity to a TaskDTO.
     * Handles conversion of associated collections like assignees, comments, and dependencies.
     * 
     * @param task The Task entity to convert
     * @return A TaskDTO containing the task data and associated elements
     */
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
        Set<UUID> dependencyIds = new HashSet<>();
        if(task.getDependencies() != null && !task.getDependencies().isEmpty()) {
            for (TaskDependency dep : task.getDependencies()) {
                if (dep.getDependsOnTask() != null) {
                    dependencyIds.add(dep.getDependsOnTask().getId());
                }
            }
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
                dependencyIds
        );
    }

     /**
     * Converts a TaskAssignee entity to a TaskAssigneeDTO.
     * 
     * @param assignee The TaskAssignee entity to convert
     * @return A TaskAssigneeDTO containing the assignee information
     */
    public static TaskAssigneeDTO assigneeToDTO(TaskAssignee assignee) {
        return new TaskAssigneeDTO(
          assignee.getId(),
          assignee.getTask().getId(),
          assignee.getUser().getId()
        );
    }

      /**
     * Converts a TaskCommentDTO to a TaskComment entity.
     * 
     * @param taskCommentDTO The TaskCommentDTO to convert
     * @return A new TaskComment entity with properties set from the DTO
     */
    public static TaskComment dtoToComment(TaskCommentDTO taskCommentDTO) {
        TaskComment comment = new TaskComment();

        comment.setComment(taskCommentDTO.comment());
        comment.setCreatedAt(taskCommentDTO.createdAt());
        return comment;
    }
    
    /**
     * Converts a TaskComment entity to a TaskCommentDTO.
     * 
     * @param createdComment The TaskComment entity to convert
     * @return A TaskCommentDTO containing the comment data
     */
    public static TaskCommentDTO commentToDTO(TaskComment createdComment) {
        return new TaskCommentDTO(
                createdComment.getId(),
                createdComment.getTask().getId(),
                createdComment.getUser().getId(),
                createdComment.getComment(),
                createdComment.getCreatedAt()
        );
    }
}

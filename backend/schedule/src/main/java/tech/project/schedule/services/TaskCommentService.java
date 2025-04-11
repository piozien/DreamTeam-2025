package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskComment;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskCommentRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.utils.GetProjectRole;
import tech.project.schedule.services.utils.PmAndAssigneeCheck;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskCommentService {
    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskComment addComment(UUID taskId, User user, TaskComment comment){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)){
            throw new ApiException("You are not allowed to add comments", HttpStatus.FORBIDDEN);
        }
        task.getComments().add(comment);
        taskRepository.save(task);
        return comment;
    }
    @Transactional
    public TaskComment deleteComment(UUID taskId, User user, UUID commentId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found.", HttpStatus.NOT_FOUND));

        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException("Comment not found.", HttpStatus.NOT_FOUND));
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId,user)){
            throw new ApiException("You are not allowed to delete this comment", HttpStatus.FORBIDDEN);
        }
        task.getComments().remove(comment);
        taskRepository.save(task);
        return comment;
    }

    @Transactional
    public void deleteAllCommentsForTask(UUID taskId) {
        taskCommentRepository.deleteAllByTask_Id(taskId);
    }

    public List<TaskComment> getCommentsForTask(UUID taskId) {
        return taskCommentRepository.findAllByTask_Id(taskId);
    }

    public List<TaskComment> getCommentsByUser(UUID userId) {
        return taskCommentRepository.findAllByUser_Id(userId);
    }

}


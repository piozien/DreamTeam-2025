package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskComment;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskCommentRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.repositories.UserRepository;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskCommentService {
    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskComment addComment(UUID taskId, UUID userId, String content) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        // ToDo Check if user is assigned to task
        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setComment(content);

        return taskCommentRepository.save(comment);
    }

    public void deleteComment(UUID commentId, UUID userId, UUID taskId) {
        // todo: add deleting single comment by comment Id and check if user is project manager or if he wrote the comment
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        /*
        boolean isPM = existingProject.getMembers().containsKey(user.getId()) &&
                ProjectUserRole.PM.equals(existingProject.getMembers().get(user.getId()).getRole());
         */


        if(!user.getGlobalRole().equals(GlobalRole.ADMIN)){
            throw new ApiException("You cannot delete this comment", HttpStatus.FORBIDDEN);
        }


        if (!taskCommentRepository.existsById(commentId)) {
            throw new ApiException("Comment not found", HttpStatus.NOT_FOUND);
        }

        taskCommentRepository.deleteById(commentId);
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


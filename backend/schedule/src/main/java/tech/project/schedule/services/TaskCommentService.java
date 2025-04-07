package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
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

    public TaskComment addComment(UUID taskId, UUID userId, String content) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        TaskComment comment = new TaskComment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setComment(content);

        return taskCommentRepository.save(comment);
    }

    public void deleteComment(UUID commentId) {
        if (!taskCommentRepository.existsById(commentId)) {
            throw new ApiException("Comment not found");
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

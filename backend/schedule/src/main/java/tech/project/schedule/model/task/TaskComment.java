package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.dto.task.TaskCommentDTO;
import tech.project.schedule.model.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Task_Comments")
@Data
public class TaskComment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static TaskCommentDTO mapToTaskCommentDTO(TaskComment comment){
        return new TaskCommentDTO(
                comment.getId(),
                comment.getTask().getId(),
                comment.getUser().getId(),
                comment.getComment(),
                comment.getCreatedAt()
        );
    }
}

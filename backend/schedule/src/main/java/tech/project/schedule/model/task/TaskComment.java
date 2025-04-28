package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.dto.task.TaskCommentDTO;
import tech.project.schedule.model.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing a comment on a task.
 * Comments allow users to provide feedback, updates, or discussion points
 * related to specific tasks, with tracking of who made the comment and when.
 */
@Entity
@Table(name = "Task_Comments")
@Data
public class TaskComment {
    /**
     * Unique identifier for the comment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The task to which this comment belongs.
     */
    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * The user who created the comment.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The text content of the comment.
     */
    @Column(nullable = false)
    private String comment;

    /**
     * Timestamp indicating when the comment was created.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Sets the creation timestamp when a comment is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

     /**
     * Converts this entity to its corresponding DTO representation.
     * 
     * @param comment The TaskComment entity to convert
     * @return A TaskCommentDTO containing the comment data
     */
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

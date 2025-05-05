package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.dto.task.TaskCommentDTO;
import tech.project.schedule.dto.task.TaskFileDTO;
import tech.project.schedule.model.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing a file attachment for a task.
 * Stores file metadata and references to associated tasks and users,
 * allowing files to be uploaded and attached to specific tasks for
 * documentation, reference, or deliverable purposes.
 */
@Entity
@Table(name = "Task_Files")
@Data
public class TaskFile {
    /**
     * Unique identifier for the file attachment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The task to which this file is attached.
     */
    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

     /**
     * The user who uploaded the file.
     */
    @ManyToOne
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

     /**
     * The path where the actual file is stored in the system.
     */
    @Column(name = "file_path", nullable = false)
    private String filePath;

     /**
     * Timestamp indicating when the file was uploaded.
     */
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    /**
     * Sets the upload timestamp when a file record is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    /**
     * Converts this entity to its corresponding DTO representation.
     * 
     * @param file The TaskFile entity to convert
     * @return A TaskFileDTO containing the file metadata
     */
    public static TaskFileDTO mapToTaskFileDTO(TaskFile file){
        return new TaskFileDTO(
                file.getId(),
                file.getTask().getId(),
                file.uploadedBy.getId(),
                file.filePath,
                file.uploadedAt
        );
    }
}

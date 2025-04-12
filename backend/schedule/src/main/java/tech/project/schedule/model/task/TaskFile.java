package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.dto.task.TaskCommentDTO;
import tech.project.schedule.dto.task.TaskFileDTO;
import tech.project.schedule.model.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Task_Files")
@Data
public class TaskFile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

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

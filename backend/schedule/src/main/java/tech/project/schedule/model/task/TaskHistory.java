package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.model.user.User;
import tech.project.schedule.model.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Task_History")
@Data
public class TaskHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false)
    private TaskStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private TaskStatus newStatus;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}

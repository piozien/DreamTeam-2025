package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.model.user.User;
import tech.project.schedule.model.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing a historical record of task status changes.
 * Serves as an audit trail that tracks the progression of tasks through
 * different statuses over time, including who made each change and when.
 */
@Entity
@Table(name = "Task_History")
@Data
public class TaskHistory {
     /**
     * Unique identifier for the history record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The task for which this history entry applies.
     */
    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * The user who changed the task status.
     */
    @ManyToOne
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    /**
     * The status of the task before the change.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", nullable = false)
    private TaskStatus oldStatus;

    /**
     * The status of the task after the change.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private TaskStatus newStatus;

     /**
     * Timestamp indicating when the status was changed.
     */
    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    /**
     * Sets the timestamp when a history record is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}

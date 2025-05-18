package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tech.project.schedule.model.user.User;

import java.util.UUID;

/**
 * Entity class representing the assignment of a user to a specific task.
 * Creates a many-to-many relationship between tasks and users by establishing 
 * which users are responsible for working on specific tasks.
 */
@Entity
@Table(name = "Task_Assignees")
@Data
@NoArgsConstructor
public class TaskAssignee {
    /**
     * Unique identifier for the task assignment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The task to which the user is assigned.
     */
    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Task task;

    /**
     * The user who is assigned to the task.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private User user;

    /**
     * The ID of the Google Calendar event associated with this task assignment.
     * Used to track and manage the calendar event when the assignment changes.
     */
    @Column(name = "calendar_event_id", length = 1024)
    private String calendarEventId;
}

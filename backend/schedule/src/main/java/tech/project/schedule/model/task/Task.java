package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tech.project.schedule.model.enums.TaskPriority;
import tech.project.schedule.model.enums.TaskStatus;
import tech.project.schedule.model.project.Project;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity class representing a task within a project.
 * Tasks are the atomic work units that make up a project, with properties
 * for tracking timeline, assignment, priority, and dependencies.
 */
@Entity
@Table(name = "Task")
@Data
@NoArgsConstructor
public class Task {
    /**
     * Unique identifier for the task.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

     /**
     * The project to which this task belongs.
     */
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Project project;

    /**
     * The name/title of the task.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Optional detailed description of the task.
     */
    private String description;

    /**
     * The date when work on the task should begin.
     */
    @Column(name = "startdate", nullable = false)
    private LocalDateTime startDate;

    /**
     * Optional date when the task should be completed.
     */
    @Column(name = "enddate")
    private LocalDateTime endDate;
    
    /**
     * The ID of the Google Calendar event associated with this task.
     */
    @Column(name = "calendar_event_id", length = 1024)
    private String calendarEventId;

    /**
     * The priority level of the task (e.g., CRITICAL, IMPORTANT, OPTIONAL).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    /**
     * The current status of the task (e.g., TO_DO, IN_PROGRESS, FINISHED).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

     /**
     * Users who are assigned to work on this task.
     */
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private Set<TaskAssignee> assignees;

    /**
     * Comments added to the task by users.
     */
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private Set<TaskComment> comments;

    /**
     * Tasks that this task depends on (prerequisites).
     */
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private Set<TaskDependency> dependencies;

    /**
     * Tasks that depend on this task (successors).
     */
    @OneToMany(mappedBy = "dependsOnTask", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private Set<TaskDependency> dependentTasks;

     /**
     * Creates a new task with essential information.
     * 
     * @param project The project this task belongs to
     * @param name The name/title of the task
     * @param description The description of the task
     * @param startDate The start date for the task
     * @param status The initial status of the task
     */
    public Task(Project project, String name, String description,
                LocalDateTime startDate, TaskStatus status) {
        this.project = project;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.status = status;
        this.assignees = new HashSet<>();
        this.comments = new HashSet<>();
        this.dependencies = new HashSet<>();
        this.dependentTasks = new HashSet<>();
    }

    /**
     * Ensures that all collection fields are properly initialized before persisting or updating.
     * Prevents null pointer exceptions when interacting with the collections.
     */
    @PrePersist
    @PreUpdate
    private void ensureCollectionsAreInitialized(){
        if(assignees == null){
            assignees = new HashSet<>();
        }
        if(comments == null){
            comments = new HashSet<>();
        }
        if(dependencies == null){
            dependencies = new HashSet<>();
        }
        if(dependentTasks == null){
            dependentTasks = new HashSet<>();
        }
    }
}

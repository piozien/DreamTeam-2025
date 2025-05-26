package tech.project.schedule.model.task;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

/**
 * Entity class representing a dependency relationship between tasks.
 * Task dependencies define the sequence or order in which tasks must be completed,
 * establishing a prerequisite relationship where one task must be completed before
 * another can begin.
 */
@Entity
@Table(name = "Task_Dependencies")
@Data
public class TaskDependency {
    /**
     * Unique identifier for the dependency relationship.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

     /**
     * The task that has a dependency (the dependent task).
     * This task cannot start until its dependency is completed.
     */
    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * The task that must be completed before the dependent task can begin.
     * This represents the prerequisite task in the relationship.
     */
    @ManyToOne
    @JoinColumn(name = "depends_on_task_id", nullable = false)
    private Task dependsOnTask;
}

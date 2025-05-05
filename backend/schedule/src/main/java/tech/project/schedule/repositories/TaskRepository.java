package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.task.Task;

import java.util.UUID;

/**
 * Repository interface for managing Task entities in the database.
 * Provides standard CRUD operations inherited from JpaRepository,
 * as well as custom methods for task-specific queries.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    /**
     * Checks if a task with the specified name already exists.
     * Useful for validating uniqueness constraints during task creation.
     * 
     * @param name The task name to check for existence
     * @return true if a task with the given name exists, false otherwise
     */
    boolean existsByName(String name);
    /**
     * Finds a task by its name.
     * 
     * @param taskName The name of the task to find
     * @return The Task with the specified name, or null if not found
     */
    Task findByName(String taskName);

    /**
     * Retrieves a task by its unique identifier.
     * Though similar to the findById method from JpaRepository,
     * this method returns the task directly rather than an Optional.
     * 
     * @param taskId The UUID of the task to retrieve
     * @return The Task with the specified ID
     */
    Task getTaskById(UUID taskId);
}

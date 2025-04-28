package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.task.TaskDependency;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing TaskDependency entities in the database.
 * Handles the prerequisite relationships between tasks, providing methods to
 * query and manipulate the directed dependencies that determine task sequencing.
 */
@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, UUID> {

    /**
     * Finds all dependencies where a specific task is the prerequisite.
     * Useful for identifying which tasks are blocked by the completion of a given task.
     * 
     * @param dependsOnTaskId The UUID of the prerequisite task
     * @return A list of TaskDependency entities where the specified task is required first
     */
    List<TaskDependency> findAllByDependsOnTaskId(UUID dependsOnTaskId);


    /**
     * Removes a specific dependency relationship between two tasks.
     * Used when modifying task sequences or removing blockers.
     * 
     * @param taskId The UUID of the dependent task
     * @param dependsOnTaskId The UUID of the prerequisite task
     */
    void deleteByTaskIdAndDependsOnTaskId(UUID taskId, UUID dependsOnTaskId);

    /**
     * Finds a specific dependency relationship between two tasks.
     * Useful for checking if one task is directly dependent on another.
     * 
     * @param taskId The UUID of the dependent task
     * @param dependsOnTaskId The UUID of the prerequisite task
     * @return The TaskDependency entity if the relationship exists, null otherwise
     */
    TaskDependency findByTaskIdAndDependsOnTaskId(UUID taskId, UUID dependsOnTaskId);
}

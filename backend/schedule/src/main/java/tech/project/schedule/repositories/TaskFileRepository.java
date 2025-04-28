package tech.project.schedule.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.task.TaskFile;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing TaskFile entities in the database.
 * Provides methods for storing, retrieving, and manipulating files associated with tasks,
 * including lookups by various criteria and file path management operations.
 */
@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, UUID> {

    /**
     * Finds a single file associated with a specific task.
     * Note: This method will only return one file even if multiple exist.
     * 
     * @param taskId The UUID of the task
     * @return The first TaskFile found for the specified task
     */
    TaskFile findByTaskId(UUID taskId);

    /**
     * Retrieves all files associated with a specific task.
     * 
     * @param taskId The UUID of the task
     * @return A list of all TaskFile entities for the specified task
     */
    List<TaskFile> findAllByTaskId(UUID taskId);

    /**
     * Finds a file by its path in the file system.
     * 
     * @param filePath The path where the file is stored
     * @return The TaskFile entity matching the specified path
     */
    TaskFile findByFilePath(String filePath) ;

    /**
     * Checks if a file with the given path exists in the database.
     * Useful for preventing duplicate file uploads.
     * 
     * @param filePath The path to check
     * @return true if a file with this path exists, false otherwise
     */
    boolean existsByFilePath(String filePath);

    /**
     * Deletes a file record by its path.
     * 
     * @param filePath The path of the file to delete
     */
    void deleteByFilePath(String filePath);

    /**
     * Deletes all files associated with a specific task.
     * Typically used when a task is being deleted.
     * 
     * @param taskId The UUID of the task whose files should be deleted
     */
    void deleteByTaskId(UUID taskId);

    /**
     * Updates the file path for all files associated with a specific task.
     * Uses a custom JPQL query with modifying annotation.
     * 
     * @param taskId The UUID of the task
     * @param filePath The new file path to set
     */
    @Modifying
    @Transactional
    @Query("UPDATE TaskFile tf SET tf.filePath = :filePath WHERE tf.task.id = :taskId")
    void setFilePathByTaskId(UUID taskId, String filePath);

    /**
     * Updates the file path for a specific file.
     * Uses a custom JPQL query with modifying annotation.
     * 
     * @param id The UUID of the file
     * @param filePath The new file path to set
     */
    @Modifying
    @Transactional
    @Query("UPDATE TaskFile tf SET tf.filePath = :filePath WHERE tf.id = :id")
    void setFilePathById(UUID id, String filePath);
}

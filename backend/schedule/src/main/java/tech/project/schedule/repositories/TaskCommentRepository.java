package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.task.TaskComment;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing TaskComment entities in the database.
 * Provides methods for retrieving, storing, and deleting task comments,
 * with specialized queries for filtering comments by task or user.
 */
@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    /**
     * Retrieves all comments associated with a specific task.
     * Useful for displaying the comment history on a task's detail view.
     * 
     * @param taskId The UUID of the task
     * @return A list of comments for the specified task
     */
    List<TaskComment> findAllByTask_Id(UUID taskId);

    /**
     * Retrieves all comments created by a specific user.
     * Can be used to track a user's communication history.
     * 
     * @param userId The UUID of the user
     * @return A list of comments created by the specified user
     */
    List<TaskComment> findAllByUser_Id(UUID userId);

    /**
     * Deletes all comments associated with a specific task.
     * Typically used when a task is being deleted to maintain data integrity.
     * 
     * @param taskId The UUID of the task whose comments should be deleted
     */
    void deleteAllByTask_Id(UUID taskId);               //delete all comments attached to task
}

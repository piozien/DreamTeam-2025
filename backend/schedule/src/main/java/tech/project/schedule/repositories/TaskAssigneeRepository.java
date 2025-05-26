package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.task.TaskAssignee;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing TaskAssignee entities in the database.
 * Handles the relationship between users and the tasks they are assigned to,
 * providing methods to query task assignments by various criteria.
 */
@Repository
public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, UUID> {

    /**
     * Retrieves all task assignments for a specific task.
     * Useful for finding all users assigned to a particular task.
     * 
     * @param taskId The UUID of the task
     * @return A list of TaskAssignee entities associated with the specified task
     */
    List<TaskAssignee> findAllByTask_Id(UUID taskId);           //All Users in single Task

    /**
     * Retrieves all task assignments for a specific user.
     * Useful for finding all tasks assigned to a particular user.
     * 
     * @param userId The UUID of the user
     * @return A list of TaskAssignee entities associated with the specified user
     */
    List<TaskAssignee> findAllByUser_Id(UUID userId);           //All Task in single User

     /**
     * Removes the assignment of a specific task from a specific user.
     * Used when unassigning a user from a task.
     * 
     * @param taskId The UUID of the task
     * @param userId The UUID of the user
     */
    void deleteByTask_IdAndUser_Id(UUID taskId, UUID userId);   //

}

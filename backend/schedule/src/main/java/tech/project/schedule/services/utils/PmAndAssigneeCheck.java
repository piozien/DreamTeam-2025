package tech.project.schedule.services.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskRepository;

import java.util.UUID;

/**
 * Utility component for checking permission combinations related to tasks.
 * Provides methods to verify if a user is either a Project Manager or an assignee
 * for a specific task, which is a common authorization check throughout the application.
 * 
 * This class uses static methods with dependency injection to maintain ease of use
 * while still leveraging Spring's dependency management capabilities.
 */
@Component
public class PmAndAssigneeCheck {
    private static TaskRepository taskRepository;

    /**
     * Setter method for injecting the TaskRepository dependency.
     * Uses Spring's @Autowired to populate the static repository reference.
     * 
     * @param taskRepository The repository for accessing task data
     */
    @Autowired
    public void setTaskRepository(TaskRepository taskRepository) {
        PmAndAssigneeCheck.taskRepository = taskRepository;
    }

    /**
     * Helper method that retrieves a task by its ID.
     * 
     * @param taskId The UUID of the task to retrieve
     * @return The task entity if found
     * @throws ApiException if the task is not found
     */
    private static Task giveMeTask(UUID taskId){
    
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task with ID " + taskId + " not found", HttpStatus.NOT_FOUND));
    }

     /**
     * Checks if a user is neither a Project Manager nor an assignee for a task.
     * This is commonly used to restrict task-related operations to only users
     * who are directly involved with the task.
     * 
     * @param taskId The ID of the task to check
     * @param user The user to check for permissions
     * @return true if the user is neither a PM nor an assignee, false otherwise
     */
    public static boolean checkIfNotPmAndAssignee(UUID taskId, User user){
        boolean isPM = GetProjectRole.getProjectRole(user, giveMeTask(taskId).getProject()) == ProjectUserRole.PM;
        boolean isAssignee = GetTaskAssignee.getAssignee(giveMeTask(taskId), user) == user;
        return !(isPM || isAssignee);
    }
}

package tech.project.schedule.services.utils;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.user.User;

/**
 * Utility class for checking if a user is assigned to a specific task.
 * Provides a convenient method to validate task assignment relationships
 * between users and tasks within the scheduling system.
 */
public class GetTaskAssignee {
    /**
     * Checks if a user is assigned to a specific task and returns the user if they are.
     * This method examines the task's assignee collection and verifies if the provided
     * user is among the assignees by matching user IDs.
     * 
     * @param task The task to check for assignees
     * @param user The user to check for assignment
     * @return The user object if they are assigned to the task, null otherwise
     */
    public static User getAssignee(Task task, User user){
         boolean isAssignee = task.getAssignees().stream().anyMatch(a -> a.getUser().getId().equals(user.getId()));
         return isAssignee ? user : null;
    }
}

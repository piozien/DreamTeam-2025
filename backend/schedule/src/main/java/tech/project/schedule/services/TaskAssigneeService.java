package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskAssigneeRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.services.utils.GetProjectRole;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

/**
 * Service class for managing task assignments to users.
 * Provides functionality for assigning users to tasks, removing assignments,
 * and retrieving assignment information with appropriate permission checks.
 */
@Service
@RequiredArgsConstructor
public class TaskAssigneeService {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;

    /**
     * Assigns a project member to a task.
     * Only Project Managers can assign members to tasks, and the user being assigned
     * must be a member of the project. Users cannot be assigned twice to the same task.
     *
     * @param taskId The ID of the task
     * @param user The user performing the assignment operation (must be a PM)
     * @param userToBeAdded The user to assign to the task
     * @return The created task assignment entity
     * @throws ApiException if task not found, user lacks permission, user not a project member, or user already assigned
     */
    @Transactional
    public TaskAssignee assignMemberToTask(UUID taskId, User user, User userToBeAdded){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isPM = GetProjectRole.getProjectRole(user, task.getProject()) == ProjectUserRole.PM;
        if(!isPM){
            throw new ApiException("You dont have permission to add assignees to this task.", HttpStatus.FORBIDDEN);
        }
        
        ProjectUserRole userRole = GetProjectRole.getProjectRole(userToBeAdded, task.getProject());
        if (userRole == null) {
            throw new ApiException("User is not a member of this project", HttpStatus.BAD_REQUEST);
        }

        boolean isAlreadyAssigned = task.getAssignees().stream()
                .anyMatch(assignee -> assignee.getUser().getId().equals(userToBeAdded.getId()));
                
        if(isAlreadyAssigned){
            throw new ApiException("This member is already assigned to this task", HttpStatus.CONFLICT);
        }

        TaskAssignee newAssignee = new TaskAssignee();
        newAssignee.setTask(task);
        newAssignee.setUser(userToBeAdded);
        task.getAssignees().add(newAssignee);
        taskRepository.save(task);
        
        TaskAssignee savedAssignee = taskAssigneeRepository.save(newAssignee);

        return savedAssignee;
    }

    /**
     * Removes a user assignment from a task.
     * Only Project Managers can remove assignees from tasks.
     *
     * @param taskId The ID of the task
     * @param assigneeId The ID of the assignment to remove
     * @param currentUser The user performing the removal operation
     * @throws ApiException if task not found, assignee not found, or user lacks permission
     */
    @Transactional
    public void removeAssigneeFromTask(UUID taskId, UUID assigneeId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        boolean isPM = GetProjectRole.getProjectRole(currentUser, task.getProject()) == ProjectUserRole.PM;
        if (!isPM) {
            throw new ApiException("You don't have permission to remove assignees from this task", HttpStatus.FORBIDDEN);
        }
        
        TaskAssignee assigneeToRemove = task.getAssignees().stream()
                .filter(assignee -> assignee.getId().equals(assigneeId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Assignee not found", HttpStatus.NOT_FOUND));
        
        task.getAssignees().remove(assigneeToRemove);
        taskRepository.save(task);
        taskAssigneeRepository.delete(assigneeToRemove);
    }

    /**
     * Retrieves all assignees for a specific task.
     * Only Project Managers and Admins can access this information.
     *
     * @param taskId The ID of the task
     * @param user The user requesting the information
     * @return List of all task assignments for the specified task
     * @throws ApiException if task not found or user lacks permission
     */
    public List<TaskAssignee> getAllAssigneesByTaskId(UUID taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        ProjectUserRole role = GetProjectRole.getProjectRole(user, task.getProject());
        boolean isPM = role == ProjectUserRole.PM;
        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
        
        if (!isPM && !isAdmin) {
            throw new ApiException("Only Project Managers and Admins can view all assignees", HttpStatus.FORBIDDEN);
        }
        
        return taskAssigneeRepository.findAllByTask_Id(taskId);
    }

    /**
     * Gets the set of assignees for a task with more relaxed permissions.
     * Users can view assignees if they are project members or if they are
     * themselves assigned to the task.
     *
     * @param taskId The ID of the task
     * @param user The user requesting the information
     * @return Set of task assignments for the specified task
     * @throws ApiException if task not found or user lacks permission
     */
    public Set<TaskAssignee> getTaskAssignees(UUID taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        ProjectUserRole role = GetProjectRole.getProjectRole(user, task.getProject());
        boolean isAssignee = task.getAssignees().stream()
                .anyMatch(assignee -> assignee.getUser().getId().equals(user.getId()));
                
        if (role == null && !isAssignee) {
            throw new ApiException("You don't have permission to view assignees for this task", HttpStatus.FORBIDDEN);
        }
        
        return new HashSet<>(task.getAssignees());
    }

}

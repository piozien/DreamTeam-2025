package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.enums.TaskStatus;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.user.User;
import tech.project.schedule.services.utils.GetProjectRole;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import tech.project.schedule.repositories.ProjectRepository;
import tech.project.schedule.repositories.TaskRepository;


/**
 * Service class for managing tasks within the scheduling system.
 * Provides core functionality for creating, retrieving, updating, and deleting tasks,
 * with appropriate permission checks and business rule validations.
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    /**
     * Creates a new task within a project.
     * Validates that the user has permission to create tasks in the project
     * and that the task dates are valid relative to the project timeline.
     *
     * @param task The task entity to create
     * @param user The user creating the task
     * @return The newly created task
     * @throws ApiException if project not found, user lacks permission, or task dates are invalid
     */
    @Transactional
    public Task createTask(Task task, User user){

        Project project = projectRepository.findById(task.getProject().getId())
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));

        ProjectUserRole role = GetProjectRole.getProjectRole(user, project);
        if(role == null){
            throw new ApiException("You don't have permission to create tasks in this project", HttpStatus.FORBIDDEN);
        }
        if(task.getStartDate().isBefore(project.getStartDate())){
            throw new ApiException("Task start date must be after project start date", HttpStatus.BAD_REQUEST);
        }
        task.setAssignees(new HashSet<>());
        task.setComments(new HashSet<>());
        task.setHistory(new HashSet<>());
        task.setDependencies(new HashSet<>());
        task.setDependentTasks(new HashSet<>());
        task.setFiles(new HashSet<>());

        Task newTask = taskRepository.save(task);
        project.getTasks().add(newTask);
        projectRepository.save(project);
        return newTask;
    }

    /**
     * Updates an existing task with new information.
     * Only project managers and users assigned to the task can update it.
     * Manages task completion by automatically setting end date when status changes.
     *
     * @param updatedTask The task entity containing updated values
     * @param taskId The ID of the task to update
     * @param user The user performing the update
     * @return The updated task entity
     * @throws ApiException if task not found, user lacks permission, or update violates business rules
     */
    @Transactional
    public Task updateTask(Task updatedTask, UUID taskId, User user) {

        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        ProjectUserRole role = GetProjectRole.getProjectRole(user, existingTask.getProject());
        boolean isAssignee = existingTask.getAssignees().stream()
                .anyMatch(assignee -> assignee.getUser().getId().equals(user.getId()));

        if (role != ProjectUserRole.PM && !isAssignee) {
            throw new ApiException("You don't have permission to update this task", HttpStatus.FORBIDDEN);
        }

        if (updatedTask.getName() != null && !updatedTask.getName().equals(existingTask.getName()) && taskRepository.existsByName(updatedTask.getName())) {
            throw new ApiException("Task by name: " + updatedTask.getName() + " already exists.", HttpStatus.CONFLICT);
        }
        if (updatedTask.getName() != null) {
            existingTask.setName(updatedTask.getName());
        }
        if (updatedTask.getDescription() != null) {
            existingTask.setDescription(updatedTask.getDescription());
        }
        if (updatedTask.getStartDate() != null) {
            if (updatedTask.getStartDate().isBefore(existingTask.getProject().getStartDate())) {
                throw new ApiException("Task start date must be after project start date", HttpStatus.BAD_REQUEST);
            }
            existingTask.setStartDate(updatedTask.getStartDate());
        }
        if (updatedTask.getPriority() != null) {
            existingTask.setPriority(updatedTask.getPriority());
        }
        if (updatedTask.getStatus() != null) {
            boolean wasCompleted = existingTask.getStatus() == TaskStatus.FINISHED;
            boolean isNowCompleted = updatedTask.getStatus() == TaskStatus.FINISHED;
            if (!wasCompleted && isNowCompleted) {
                existingTask.setEndDate(LocalDate.now());
            }
            if (wasCompleted && !isNowCompleted) {
                existingTask.setEndDate(null);
            }
            existingTask.setStatus(updatedTask.getStatus());
        }

        return taskRepository.save(existingTask);
    }

    /**
     * Deletes a task from the system.
     * Only project managers can delete tasks.
     *
     * @param taskId The ID of the task to delete
     * @param user The user attempting to delete the task
     * @throws ApiException if task not found or user lacks permission
     */
    @Transactional
    public void deleteTask(UUID taskId, User user){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isPM = GetProjectRole.getProjectRole(user, task.getProject()) == ProjectUserRole.PM;
        if(!isPM){
            throw new ApiException("You cannot delete this task", HttpStatus.FORBIDDEN);
        }
        taskRepository.deleteById(taskId);
    }

    /**
     * Retrieves a task by its ID.
     * Only project members and users assigned to the task can view it.
     *
     * @param taskId The ID of the task to retrieve
     * @param user The user requesting the task
     * @return The requested task entity
     * @throws ApiException if task not found or user lacks permission
     */
    public Task getTaskById(UUID taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        
        ProjectUserRole role = GetProjectRole.getProjectRole(user, task.getProject());
        boolean isAssignee = task.getAssignees().stream()
                .anyMatch(assignee -> assignee.getUser().getId().equals(user.getId()));
        
        if (role == null && !isAssignee) {
            throw new ApiException("You don't have permission to view this task", HttpStatus.FORBIDDEN);
        }
        
        return task;
    }

    /**
     * Retrieves all tasks for a specific project.
     * Only project members can view project tasks.
     *
     * @param projectId The ID of the project
     * @param user The user requesting the tasks
     * @return List of tasks in the specified project
     * @throws ApiException if project not found or user lacks permission
     */
    public List<Task> getTasksByProject(UUID projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));
        
        ProjectUserRole role = GetProjectRole.getProjectRole(user, project);
        if (role == null) {
            throw new ApiException("You don't have permission to view tasks in this project", HttpStatus.FORBIDDEN);
        }
        
        return new ArrayList<>(project.getTasks());
    }
}

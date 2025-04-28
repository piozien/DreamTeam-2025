package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskDependency;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskDependencyRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.services.utils.PmAndAssigneeCheck;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing task dependencies within the scheduling system.
 * Provides functionality for creating, updating, removing, and retrieving task 
 * dependencies, ensuring proper sequencing of work and prerequisite relationships
 * between tasks.
 */
@Service
@RequiredArgsConstructor
public class TaskDependencyService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;

    /**
     * Creates a new dependency relationship between two tasks.
     * Establishes that one task (dependsOnTask) must be completed before another (task).
     * Only project managers and users assigned to the task can add dependencies.
     *
     * @param taskId The ID of the dependent task (the one that needs to wait)
     * @param dependencyId The ID of the prerequisite task (the one that must be completed first)
     * @param user The user attempting to create the dependency
     * @throws ApiException if tasks not found, user lacks permission, or dependency already exists
     */
    @Transactional
    public void addDependency(UUID taskId, UUID dependencyId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        Task dependsOnTask = taskRepository.findById(dependencyId)
                .orElseThrow(() -> new ApiException("Dependency task not found", HttpStatus.NOT_FOUND));

        TaskDependency existingDependency = taskDependencyRepository
                .findByTaskIdAndDependsOnTaskId(taskId, dependencyId);

        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)){
            throw new ApiException("You are not allowed to add dependencies", HttpStatus.FORBIDDEN);
        }

        if (existingDependency != null) {
            throw new ApiException("Dependency already exists", HttpStatus.BAD_REQUEST);
        }

        TaskDependency taskDependency = new TaskDependency();
        taskDependency.setTask(task);
        taskDependency.setDependsOnTask(dependsOnTask);

        task.getDependencies().add(taskDependency);

        taskDependencyRepository.save(taskDependency);
    }

    /**
     * Removes an existing dependency relationship between two tasks.
     * Only project managers and users assigned to the task can remove dependencies.
     *
     * @param taskId The ID of the dependent task
     * @param dependencyId The ID of the prerequisite task
     * @param user The user attempting to remove the dependency
     * @throws ApiException if task not found, dependency not found, or user lacks permission
     */
    @Transactional
    public void removeDependency(UUID taskId, UUID dependencyId, User user) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)){
            throw new ApiException("You are not allowed to add dependencies", HttpStatus.FORBIDDEN);
        }

        TaskDependency taskDependency = taskDependencyRepository
                .findByTaskIdAndDependsOnTaskId(taskId, dependencyId);

        if (taskDependency == null) {
            throw new ApiException("Dependency not found", HttpStatus.NOT_FOUND);
        }


        task.getDependencies().remove(taskDependency);
        taskRepository.save(task);
        taskDependencyRepository.delete(taskDependency);
    }

    /**
     * Updates an existing task dependency with a new prerequisite task.
     * Only project managers and users assigned to the task can update dependencies.
     *
     * @param taskId The ID of the dependent task
     * @param dependencyId The ID of the current prerequisite task
     * @param user The user attempting to update the dependency
     * @throws ApiException if tasks not found, dependency not found, or user lacks permission
     */
    @Transactional
    public void updateTaskDependency(UUID taskId, UUID dependencyId, User user) {
        Task newTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        TaskDependency taskDependency = taskDependencyRepository
                .findByTaskIdAndDependsOnTaskId(taskId, dependencyId);

        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)){
            throw new ApiException("You are not allowed to add dependencies", HttpStatus.FORBIDDEN);
        }

        if(taskDependency == null) {
            throw new ApiException("Dependency not found", HttpStatus.NOT_FOUND);
        }
        taskDependency.setDependsOnTask(newTask);
        taskDependencyRepository.save(taskDependency);
    }

    /**
     * Retrieves all prerequisite tasks for a specific task.
     * Only project managers and users assigned to the task can view dependencies.
     *
     * @param taskId The ID of the task whose dependencies are being retrieved
     * @param user The user requesting the dependencies
     * @return Set of tasks that must be completed before the specified task
     * @throws ApiException if task not found or user lacks permission
     */
    public Set<Task> getTaskDependencies(UUID taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)){
            throw new ApiException("You are not allowed to view dependencies", HttpStatus.FORBIDDEN);
        }
        
        Set<Task> dependencies = new HashSet<>();
        task.getDependencies().forEach(dependency -> {
            dependencies.add(dependency.getDependsOnTask());
        });
        
        return dependencies;
    }

}

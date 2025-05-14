package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskDependency;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskDependencyRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.services.utils.NotificationHelper;
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
    private final NotificationHelper notificationHelper;

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
        
        // Powiadom osobę dodającą zależność
        notificationHelper.notifyUser(
            user,
            NotificationStatus.TASK_DEPENDENCY_ADDED,
            "Dodano zależność: zadanie " + task.getName() + " zależy teraz od zadania " + dependsOnTask.getName()
        );
        
        // Powiadom przypisanych użytkowników zadania
        task.getAssignees().forEach(assignee -> {
            // Nie powiadamiaj ponownie osoby dodającej zależność
            if (!assignee.getUser().getId().equals(user.getId())) {
                notificationHelper.notifyTaskAssignee(
                    assignee.getUser(),
                    NotificationStatus.TASK_DEPENDENCY_ADDED,
                    task.getName()
                );
            }
        });
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

        // Zapisz nazwę zadania zależnego przed usunięciem
        String dependencyTaskName = taskDependency.getDependsOnTask().getName();

        task.getDependencies().remove(taskDependency);
        taskRepository.save(task);
        taskDependencyRepository.delete(taskDependency);
        
        // Powiadom przypisanych użytkowników o usunięciu zależności
        task.getAssignees().forEach(assignee -> {
            // Nie powiadamiaj ponownie osoby usuwającej zależność
            if (!assignee.getUser().getId().equals(user.getId())) {
                notificationHelper.notifyUser(
                    assignee.getUser(),
                    NotificationStatus.TASK_DEPENDENCY_DELETED,
                    "Usunięto zależność pomiędzy zadaniami: " + task.getName() + " i " + dependencyTaskName
                );
            }
        });
    }

    /**
     * Updates an existing task dependency with a new prerequisite task.
     * Only project managers and users assigned to the task can update dependencies.
     *
     * @param taskId The ID of the dependent task
     * @param oldDependencyId The ID of the current prerequisite task to be replaced
     * @param newDependencyId The ID of the new prerequisite task to use as replacement
     * @param user The user attempting to update the dependency
     * @throws ApiException if tasks not found, dependency not found, or user lacks permission
     */
    @Transactional
    public void updateTaskDependency(UUID taskId, UUID oldDependencyId, UUID newDependencyId, User user) {
        // Find the task that has dependencies
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        // Find the new dependency task
        Task newDependencyTask = taskRepository.findById(newDependencyId)
                .orElseThrow(() -> new ApiException("New dependency task not found", HttpStatus.NOT_FOUND));
                
        // Find the existing dependency relationship
        TaskDependency taskDependency = taskDependencyRepository
                .findByTaskIdAndDependsOnTaskId(taskId, oldDependencyId);

        // Check if user has permission
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)){
            throw new ApiException("You are not allowed to modify dependencies", HttpStatus.FORBIDDEN);
        }

        // Verify the dependency exists
        if(taskDependency == null) {
            throw new ApiException("Dependency not found", HttpStatus.NOT_FOUND);
        }
        
        if (taskId.equals(newDependencyId)) {
            throw new ApiException("A task cannot depend on itself", HttpStatus.BAD_REQUEST);
        }
        
        // Zapisz nazwy zadań przed aktualizacją
        String oldDependencyName = taskDependency.getDependsOnTask().getName();
        
        // Replace the old dependency with the new one
        taskDependency.setDependsOnTask(newDependencyTask);
        taskDependencyRepository.save(taskDependency);
        
        // Powiadom przypisanych użytkowników o aktualizacji zależności
        task.getAssignees().forEach(assignee -> {
            // Nie powiadamiaj ponownie osoby aktualizującej zależność
            if (!assignee.getUser().getId().equals(user.getId())) {
                notificationHelper.notifyUser(
                    assignee.getUser(),
                    NotificationStatus.TASK_DEPENDENCY_UPDATED,
                    "Zaktualizowano zależność dla zadania " + task.getName() + ": teraz zależy od " + 
                    newDependencyTask.getName() + " (wcześniej: " + oldDependencyName + ")"
                );
            }
        });
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

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

@Service
@RequiredArgsConstructor
public class TaskDependencyService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;


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

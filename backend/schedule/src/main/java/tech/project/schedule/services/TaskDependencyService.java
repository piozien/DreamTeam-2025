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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskDependencyService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;


    public void addTaskDependency(UUID taskId, UUID dependsOnTaskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        Task dependsOnTask = taskRepository.findById(dependsOnTaskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        TaskDependency existingDependency = taskDependencyRepository
                .findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId);

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

        taskDependencyRepository.save(taskDependency);
    }


    public void removeTaskDependency(UUID taskId, UUID dependsOnTaskId, User user) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)){
            throw new ApiException("You are not allowed to add dependencies", HttpStatus.FORBIDDEN);
        }

        TaskDependency taskDependency = taskDependencyRepository
                .findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId);

        if (taskDependency == null) {
            throw new ApiException("Dependency not found", HttpStatus.NOT_FOUND);
        }


        task.getDependencies().remove(taskDependency);
        taskRepository.save(task);
        taskDependencyRepository.delete(taskDependency);
    }

    public void updateTaskDependency(UUID taskId, UUID dependsOnTaskId, User user) {
        Task newTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        TaskDependency taskDependency = taskDependencyRepository
                .findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId);

        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)){
            throw new ApiException("You are not allowed to add dependencies", HttpStatus.FORBIDDEN);
        }

        if(taskDependency == null) {
            throw new ApiException("Dependency not found", HttpStatus.NOT_FOUND);
        }
        taskDependency.setDependsOnTask(newTask);
        taskDependencyRepository.save(taskDependency);
    }

}

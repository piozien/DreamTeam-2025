package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskDependency;
import tech.project.schedule.repositories.TaskDependencyRepository;
import tech.project.schedule.repositories.TaskRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskDependencyService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;


    public TaskDependency addTaskDependency(UUID taskId, UUID dependsOnTaskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        Task dependsOnTask = taskRepository.findById(dependsOnTaskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        TaskDependency existingDependency = taskDependencyRepository
                .findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId);

        if (existingDependency != null) {
            throw new ApiException("Dependency already exists", HttpStatus.BAD_REQUEST);
        }

        TaskDependency taskDependency = new TaskDependency();
        taskDependency.setTask(task);
        taskDependency.setDependsOnTask(dependsOnTask);

        return taskDependencyRepository.save(taskDependency);
    }


    public void removeTaskDependency(UUID taskId, UUID dependsOnTaskId) {

        TaskDependency taskDependency = taskDependencyRepository
                .findByTaskIdAndDependsOnTaskId(taskId, dependsOnTaskId);

        if (taskDependency == null) {
            throw new ApiException("Dependency not found", HttpStatus.NOT_FOUND);
        }

        taskDependencyRepository.delete(taskDependency);
    }

}

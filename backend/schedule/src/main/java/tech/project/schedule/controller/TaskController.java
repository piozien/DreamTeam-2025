package tech.project.schedule.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.project.schedule.dto.task.TaskDTO;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.ProjectRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.services.TaskService;

import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@Validated @RequestBody TaskDTO taskDTO,
                                              @AuthenticationPrincipal User user) {
        Project project = projectRepository.findById(taskDTO.projectId())
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));

        Task task = new Task();
        task.setProject(project);
        task.setName(taskDTO.name());
        task.setDescription(taskDTO.description());
        task.setStartDate(taskDTO.startDate());
        task.setEndDate(taskDTO.endDate());
        task.setPriority(taskDTO.priority());
        task.setStatus(taskDTO.status());
        task.setAssignees(new HashSet<>());
        task.setComments(new HashSet<>());
        task.setHistory(new HashSet<>());
        task.setDependencies(new HashSet<>());
        task.setDependentTasks(new HashSet<>());
        task.setFiles(new HashSet<>());

        Task createdTask = taskService.createTask(task, user);

        TaskDTO createdTaskDTO = new TaskDTO(
                createdTask.getId(),
                createdTask.getProject().getId(),
                createdTask.getName(),
                createdTask.getDescription(),
                createdTask.getStartDate(),
                createdTask.getEndDate(),
                createdTask.getPriority(),
                createdTask.getStatus(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet()
        );

        return new ResponseEntity<>(createdTaskDTO, HttpStatus.CREATED);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable UUID taskId,
                                              @Validated @RequestBody TaskDTO taskDTO,
                                              @AuthenticationPrincipal User user) {
        Task task = new Task();
        task.setName(taskDTO.name());
        task.setDescription(taskDTO.description());
        task.setStartDate(taskDTO.startDate());
        task.setEndDate(taskDTO.endDate());
        task.setPriority(taskDTO.priority());
        task.setStatus(taskDTO.status());

        Task updatedTask = taskService.updateTask(task, taskId, user);

        TaskDTO updatedTaskDTO = new TaskDTO(
                updatedTask.getId(),
                updatedTask.getProject().getId(),
                updatedTask.getName(),
                updatedTask.getDescription(),
                updatedTask.getStartDate(),
                updatedTask.getEndDate(),
                updatedTask.getPriority(),
                updatedTask.getStatus(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet()
        );

        return ResponseEntity.ok(updatedTaskDTO);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDTO> getTask(@PathVariable UUID taskId) {
        Task task = taskRepository.getTaskById(taskId);

        TaskDTO taskDTO = new TaskDTO(
                task.getId(),
                task.getProject().getId(),
                task.getName(),
                task.getDescription(),
                task.getStartDate(),
                task.getEndDate(),
                task.getPriority(),
                task.getStatus(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet()
        );

        return ResponseEntity.ok(taskDTO);
    }

//    @DeleteMapping("/{taskId}")
//    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId,
//                                           @AuthenticationPrincipal User user) {
//        taskService.deleteTask(taskId, user);
//        return ResponseEntity.noContent().build();
//    }
}
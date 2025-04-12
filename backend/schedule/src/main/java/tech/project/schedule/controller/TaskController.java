package tech.project.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.project.schedule.dto.mappers.TaskMapper;
import tech.project.schedule.dto.task.TaskAssigneeDTO;
import tech.project.schedule.dto.task.TaskDTO;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.ProjectService;
import tech.project.schedule.services.TaskService;
import tech.project.schedule.services.TaskAssigneeService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskAssigneeService taskAssigneeService;

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(
            @Valid @RequestBody TaskDTO taskDTO,
            @RequestParam UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Task task = TaskMapper.dtoToTask(taskDTO);

        Task createdTask = taskService.createTask(task, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.taskToDTO(createdTask));

    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDTO> getTask(
            @PathVariable UUID taskId,
            @RequestParam UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        Task task = taskService.getTaskById(taskId, user);

        return ResponseEntity.ok(TaskMapper.taskToDTO(task));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskDTO taskDTO,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        Task task = TaskMapper.dtoToTask(taskDTO);
        Task updatedTask = taskService.updateTask(task, taskId,user);

        return ResponseEntity.ok(TaskMapper.taskToDTO(updatedTask));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        taskService.deleteTask(taskId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}")
    public ResponseEntity<TaskAssigneeDTO> addAssignee(
            @PathVariable UUID taskId,
            @RequestBody Map<String, Object> requestBody,
            @RequestParam UUID currentUserId
    ) {
        User currUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UUID userId = UUID.fromString((String) requestBody.get("userId"));

        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        TaskAssignee addedAssignee = taskAssigneeService.assignMemberToTask(taskId,currUser,userToAdd);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.assigneeToDTO(addedAssignee));
    }





}

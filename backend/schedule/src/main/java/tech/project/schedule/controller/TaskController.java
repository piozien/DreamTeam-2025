package tech.project.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.TaskService;
import tech.project.schedule.services.TaskAssigneeService;
import tech.project.schedule.services.TaskDependencyService;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final TaskAssigneeService taskAssigneeService;
    private final TaskDependencyService taskDependencyService;

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

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProject(
            @PathVariable UUID projectId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        List<Task> tasks = taskService.getTasksByProject(projectId, user);
        List<TaskDTO> taskDTOs = tasks.stream()
                .map(TaskMapper::taskToDTO)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(taskDTOs);
    }

    @PostMapping("/{taskId}/assignees")
    public ResponseEntity<TaskAssigneeDTO> addAssignee(
            @PathVariable UUID taskId,
            @RequestBody TaskAssigneeDTO assigneeDTO,
            @RequestParam UUID currentUserId
    ) {
        User currUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        UUID userId = assigneeDTO.userId();
        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        TaskAssignee addedAssignee = taskAssigneeService.assignMemberToTask(taskId, currUser, userToAdd);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.assigneeToDTO(addedAssignee));
    }
    
    @DeleteMapping("/{taskId}/assignees/{assigneeId}")
    public ResponseEntity<Void> removeAssignee(
            @PathVariable UUID taskId,
            @PathVariable UUID assigneeId,
            @RequestParam UUID currentUserId
    ) {
        User currUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
                
        taskAssigneeService.removeAssigneeFromTask(taskId, assigneeId, currUser);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{taskId}/assignees")
    public ResponseEntity<Set<TaskAssigneeDTO>> getTaskAssignees(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
                
        Set<TaskAssignee> assignees = taskAssigneeService.getTaskAssignees(taskId, user);
        Set<TaskAssigneeDTO> assigneeDTOs = assignees.stream()
                .map(TaskMapper::assigneeToDTO)
                .collect(Collectors.toSet());
                
        return ResponseEntity.ok(assigneeDTOs);
    }
    
    @PostMapping("/{taskId}/dependencies/{dependencyId}")
    public ResponseEntity<Void> addDependency(
            @PathVariable UUID taskId,
            @PathVariable UUID dependencyId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
                
        taskDependencyService.addDependency(taskId, dependencyId, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    @DeleteMapping("/{taskId}/dependencies/{dependencyId}")
    public ResponseEntity<Void> removeDependency(
            @PathVariable UUID taskId,
            @PathVariable UUID dependencyId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
                
        taskDependencyService.removeDependency(taskId, dependencyId, user);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{taskId}/dependencies")
    public ResponseEntity<Set<TaskDTO>> getTaskDependencies(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
                
        Set<Task> dependencies = taskDependencyService.getTaskDependencies(taskId, user);
        Set<TaskDTO> dependencyDTOs = dependencies.stream()
                .map(TaskMapper::taskToDTO)
                .collect(Collectors.toSet());
                
        return ResponseEntity.ok(dependencyDTOs);
    }
    
    @PutMapping("/{taskId}/dependencies/{dependencyId}")
    public ResponseEntity<Void> updateDependency(
            @PathVariable UUID taskId,
            @PathVariable UUID dependencyId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
                
        taskDependencyService.updateTaskDependency(taskId, dependencyId, user);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{taskId}/all-assignees")
    public ResponseEntity<List<TaskAssigneeDTO>> getAllTaskAssignees(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
                
        List<TaskAssignee> assignees = taskAssigneeService.getAllAssigneesByTaskId(taskId, user);
        List<TaskAssigneeDTO> assigneeDTOs = assignees.stream()
                .map(TaskMapper::assigneeToDTO)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(assigneeDTOs);
    }
}

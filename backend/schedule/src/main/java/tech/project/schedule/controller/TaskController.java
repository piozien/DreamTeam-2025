package tech.project.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.project.schedule.dto.mappers.TaskMapper;
import tech.project.schedule.dto.task.TaskAssigneeDTO;
import tech.project.schedule.dto.task.TaskCommentDTO;
import tech.project.schedule.dto.task.TaskDTO;
import tech.project.schedule.dto.task.TaskFileDTO;
import tech.project.schedule.dto.task.TaskRequestDTO;
import tech.project.schedule.dto.task.TaskUpdateDTO;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.task.TaskComment;
import tech.project.schedule.model.task.TaskFile;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.TaskService;
import tech.project.schedule.services.TaskAssigneeService;
import tech.project.schedule.services.TaskDependencyService;
import tech.project.schedule.services.TaskCommentService;
import tech.project.schedule.services.TaskFileService;

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
    private final TaskCommentService taskCommentService;
    private final TaskFileService taskFileService;

    @PostMapping
    public ResponseEntity<TaskDTO> createTask(
            @Valid @RequestBody TaskRequestDTO taskRequestDTO,
            @RequestParam UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        Task task = TaskMapper.requestDtoToTask(taskRequestDTO);

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
            @RequestBody TaskUpdateDTO taskUpdateDTO,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        Task task = TaskMapper.updateDtoToTask(taskUpdateDTO);
        Task updatedTask = taskService.updateTask(task, taskId, user);

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

    @GetMapping("dependencies/{taskId}/dependencies")
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

    @PostMapping("/{taskId}/comments")
    public ResponseEntity<TaskCommentDTO> addComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskCommentDTO taskCommentDTO,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        TaskComment comment = TaskMapper.dtoToComment(taskCommentDTO);

        TaskComment createdComment = taskCommentService.addComment(taskId, user, comment);

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.commentToDTO(createdComment));

    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<TaskCommentDTO> getComment(
            @PathVariable UUID commentId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        TaskComment comment = taskCommentService.getCommentById(commentId, user);
        return ResponseEntity.ok(TaskMapper.commentToDTO(comment));
    }

    @GetMapping("/comments/user")
    public ResponseEntity<List<TaskCommentDTO>> getUserComments(
            @RequestParam UUID userId,
            @RequestParam UUID otherUserId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        List<TaskComment> comments = taskCommentService.getUserComments(user, otherUser);
        List<TaskCommentDTO> commentDTOs = comments.stream()
                .map(TaskMapper::commentToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commentDTOs);
    }

    @GetMapping("/{taskId}/comments")
    public ResponseEntity<List<TaskCommentDTO>> getTaskComments(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        List<TaskComment> comments = taskCommentService.getCommentsForTask(taskId, user);
        List<TaskCommentDTO> commentDTOs = comments.stream()
                .map(TaskMapper::commentToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commentDTOs);
    }

    @DeleteMapping("/{taskId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        taskCommentService.deleteComment(taskId, user, commentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{taskId}/comments")
    public ResponseEntity<Void> deleteAllCommentsInTask(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        taskCommentService.deleteAllCommentsForTask(taskId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{taskId}/files")
    public ResponseEntity<TaskFileDTO> addFile(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskFileDTO taskFileDTO,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        TaskFile file = TaskMapper.dtoToFile(taskFileDTO);
        TaskFile createdFile = taskFileService.addTaskFile(taskId, user, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.fileToDTO(createdFile));

    }

    @GetMapping("/{taskId}/files/{fileId}")
    public ResponseEntity<TaskFileDTO> getTaskFileByTaskId(
            @PathVariable UUID taskId,
            @PathVariable UUID fileId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        TaskFile file = taskFileService.getTaskFileById(taskId, fileId, user);
        return ResponseEntity.ok(TaskMapper.fileToDTO(file));
    }

    @GetMapping("/{taskId}/files")
    public ResponseEntity<List<TaskFileDTO>> getTaskFiles(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        List<TaskFile> files = taskFileService.getTaskFiles(taskId, user);
        List<TaskFileDTO> fileDTOs = files.stream()
                .map(TaskMapper::fileToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileDTOs);
    }

    @DeleteMapping("/{taskId}/files/{fileId}")
    public ResponseEntity<Void> deleteTaskFile(
            @PathVariable UUID taskId,
            @PathVariable UUID fileId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        taskFileService.deleteTaskFile(taskId, fileId, user);
        return ResponseEntity.noContent().build();
    }


}

package tech.project.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.project.schedule.dto.mappers.TaskMapper;
import tech.project.schedule.dto.task.*;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.task.TaskComment;
import tech.project.schedule.model.task.TaskFile;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import tech.project.schedule.utils.UserUtils;

/**
 * Controller responsible for managing task-related operations.
 * This controller provides endpoints for creating, retrieving, updating, and deleting tasks,
 * as well as managing task assignees, dependencies, comments, and associated files.
 * It serves as the REST API interface for all task management functionality.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Transactional
public class TaskController {
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final TaskAssigneeService taskAssigneeService;
    private final TaskDependencyService taskDependencyService;
    private final TaskCommentService taskCommentService;
    private final TaskFileService taskFileService;
    
    /**
     * Creates a new task.
     *
     * @param taskRequestDTO Data transfer object containing task details
     * @param userId ID of the user creating the task
     * @return ResponseEntity containing the created task as DTO with HTTP status 201 (CREATED)
     * @throws ApiException if the user is not found
     */
    @PostMapping
    public ResponseEntity<TaskDTO> createTask(
            @Valid @RequestBody TaskRequestDTO taskRequestDTO,
            @RequestParam UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        Task task = TaskMapper.requestDtoToTask(taskRequestDTO);

        Task createdTask = taskService.createTask(task, user);

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.taskToDTO(createdTask));
    }
    
      /**
     * Retrieves a specific task by its ID.
     *
     * @param taskId ID of the task to retrieve
     * @param userId ID of the user requesting the task
     * @return ResponseEntity containing the task as DTO
     * @throws ApiException if the user or task is not found, or if user lacks access
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDTO> getTask(
            @PathVariable UUID taskId,
            @RequestParam UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);
        Task task = taskService.getTaskById(taskId, user);

        return ResponseEntity.ok(TaskMapper.taskToDTO(task));
    }
    
    /**
     * Updates an existing task.
     *
     * @param taskId ID of the task to update
     * @param taskUpdateDTO Data transfer object containing updated task details
     * @param userId ID of the user performing the update
     * @return ResponseEntity containing the updated task as DTO
     * @throws ApiException if the user or task is not found, or if user lacks permissions
     */
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDTO> updateTask(
            @PathVariable UUID taskId,
            @RequestBody TaskUpdateDTO taskUpdateDTO,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);
        Task task = TaskMapper.updateDtoToTask(taskUpdateDTO);
        Task updatedTask = taskService.updateTask(taskId, task, user);

        return ResponseEntity.ok(TaskMapper.taskToDTO(updatedTask));
    }

    
    /**
     * Deletes a task.
     *
     * @param taskId ID of the task to delete
     * @param userId ID of the user performing the deletion
     * @return ResponseEntity with HTTP status 204 (NO CONTENT) on successful deletion
     * @throws ApiException if the user or task is not found, or if user lacks permissions
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);
        taskService.deleteTask(taskId, user);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Retrieves all tasks belonging to a specific project.
     *
     * @param projectId ID of the project whose tasks are to be retrieved
     * @param userId ID of the user requesting the tasks
     * @return ResponseEntity containing a list of tasks as DTOs
     * @throws ApiException if the user is not found or lacks access to the project
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskDTO>> getTasksByProject(
            @PathVariable UUID projectId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        List<Task> tasks = taskService.getTasksByProject(projectId, user);
        List<TaskDTO> taskDTOs = tasks.stream()
                .map(TaskMapper::taskToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(taskDTOs);
    }
    
     /**
     * Assigns a user to a task.
     *
     * @param taskId ID of the task to assign the user to
     * @param assigneeDTO Data transfer object containing the user ID to assign
     * @param currentUserId ID of the user performing the assignment action
     * @return ResponseEntity containing the created task assignee as DTO with HTTP status 201 (CREATED)
     * @throws ApiException if users are not found, task doesn't exist, or current user lacks permissions
     */
    @PostMapping("/{taskId}/assignees")
    public ResponseEntity<TaskAssigneeDTO> addAssignee(
            @PathVariable UUID taskId,
            @RequestBody TaskAssigneeDTO assigneeDTO,
            @RequestParam UUID currentUserId
    ) {
        User currUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(currUser);

        UUID userId = assigneeDTO.userId();
        User userToAdd = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        TaskAssignee addedAssignee = taskAssigneeService.assignMemberToTask(taskId, currUser, userToAdd);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.assigneeToDTO(addedAssignee));
    }

    /**
     * Removes a user assignment from a task.
     *
     * @param taskId ID of the task
     * @param assigneeId ID of the assignee to remove
     * @param currentUserId ID of the user performing the removal action
     * @return ResponseEntity with HTTP status 204 (NO CONTENT) on successful removal
     * @throws ApiException if users are not found, task doesn't exist, or current user lacks permissions
     */
    @DeleteMapping("/{taskId}/assignees/{assigneeId}")
    public ResponseEntity<Void> removeAssignee(
            @PathVariable UUID taskId,
            @PathVariable UUID assigneeId,
            @RequestParam UUID currentUserId
    ) {
        User currUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(currUser);

        taskAssigneeService.removeAssigneeFromTask(taskId, assigneeId, currUser);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Retrieves all assignees for a specific task.
     *
     * @param taskId ID of the task
     * @param userId ID of the user requesting the assignee list
     * @return ResponseEntity containing a set of task assignees as DTOs
     * @throws ApiException if the user is not found, task doesn't exist, or user lacks access
     */
    @GetMapping("/{taskId}/assignees")
    public ResponseEntity<Set<TaskAssigneeDTO>> getTaskAssignees(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        Set<TaskAssignee> assignees = taskAssigneeService.getTaskAssignees(taskId, user);
        Set<TaskAssigneeDTO> assigneeDTOs = assignees.stream()
                .map(TaskMapper::assigneeToDTO)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(assigneeDTOs);
    }

     /**
     * Adds a dependency relationship between two tasks.
     *
     * @param taskId ID of the task that will depend on another task
     * @param dependentOnTaskId ID of the task that is depended upon (prerequisite task)
     * @param userId ID of the user creating the dependency
     * @return ResponseEntity with HTTP status 201 (CREATED) on successful creation
     * @throws ApiException if the user is not found, tasks don't exist, or user lacks permissions
     */
    @PostMapping("/{taskId}/dependencies/{dependentOnTaskId}")
    public ResponseEntity<Void> addDependency(
            @PathVariable UUID taskId,
            @PathVariable UUID dependentOnTaskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        taskDependencyService.addDependency(taskId, dependentOnTaskId, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

     /**
     * Removes a dependency relationship between two tasks.
     *
     * @param taskId ID of the task that depends on another
     * @param dependentOnTaskId ID of the prerequisite task to remove as dependency
     * @param userId ID of the user removing the dependency
     * @return ResponseEntity with HTTP status 204 (NO CONTENT) on successful removal
     * @throws ApiException if the user is not found, tasks don't exist, or user lacks permissions
     */
    @DeleteMapping("/{taskId}/dependencies/{dependentOnTaskId}")
    public ResponseEntity<Void> removeDependency(
            @PathVariable UUID taskId,
            @PathVariable UUID dependentOnTaskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        taskDependencyService.removeDependency(taskId, dependentOnTaskId, user);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Retrieves all dependencies for a specific task.
     *
     * @param taskId ID of the task whose dependencies are to be retrieved
     * @param userId ID of the user requesting the dependencies
     * @return ResponseEntity containing a set of UUIDs identifying the tasks that the specified task depends on
     * @throws ApiException if the user is not found, task doesn't exist, or user lacks access
     */
    @GetMapping("/{taskId}/dependencies")
    public ResponseEntity<Set<UUID>> getTaskDependencies(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        Set<Task> dependencies = taskDependencyService.getTaskDependencies(taskId, user);
        Set<UUID> dependencyIds = dependencies.stream()
                .map(Task::getId)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(dependencyIds);
    }
    /**
     * Updates a dependency relationship between tasks.
     * Replaces one prerequisite task with another for a specific task.
     *
     * @param taskId ID of the task whose dependency is being updated
     * @param oldDependentOnTaskId ID of the current prerequisite task to be replaced
     * @param request DTO containing the new prerequisite task ID
     * @param userId ID of the user updating the dependency (for authorization)
     * @return ResponseEntity with HTTP status 200 (OK) on successful update
     * @throws ApiException if the user is not found, tasks don't exist, or user lacks permissions
     */
    @PutMapping("/{taskId}/dependencies/{oldDependentOnTaskId}")
    public ResponseEntity<Void> updateDependency(
            @PathVariable UUID taskId,
            @PathVariable UUID oldDependentOnTaskId,
            @RequestBody UpdateTaskDependencyRequest request,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        taskDependencyService.updateTaskDependency(taskId, oldDependentOnTaskId, request.getNewDependentOnTaskId(), user);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Adds a comment to a task.
     *
     * @param taskId ID of the task to comment on
     * @param taskCommentDTO Data transfer object containing comment details
     * @param userId ID of the user adding the comment
     * @return ResponseEntity containing the created comment as DTO with HTTP status 201 (CREATED)
     * @throws ApiException if the user is not found, task doesn't exist, or user lacks permissions
     */
    @PostMapping("/{taskId}/comments")
    public ResponseEntity<TaskCommentDTO> addComment(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskCommentDTO taskCommentDTO,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        TaskComment comment = TaskMapper.dtoToComment(taskCommentDTO);

        TaskComment createdComment = taskCommentService.addComment(taskId, user, comment);

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.commentToDTO(createdComment));

    }
    
    /**
     * Retrieves a specific comment by its ID.
     *
     * @param commentId ID of the comment to retrieve
     * @param userId ID of the user requesting the comment
     * @return ResponseEntity containing the comment as DTO
     * @throws ApiException if the user or comment is not found, or if user lacks access
     */
    @GetMapping("/comments/{commentId}")
    public ResponseEntity<TaskCommentDTO> getComment(
            @PathVariable UUID commentId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        TaskComment comment = taskCommentService.getCommentById(commentId, user);
        return ResponseEntity.ok(TaskMapper.commentToDTO(comment));
    }
    
    /**
     * Retrieves all assignees for a specific task in list format.
     *
     * @param taskId ID of the task
     * @param userId ID of the user requesting the assignee list
     * @return ResponseEntity containing a list of task assignees as DTOs
     * @throws ApiException if the user is not found, task doesn't exist, or user lacks access
     */
    @GetMapping("/{taskId}/all-assignees")
    public ResponseEntity<List<TaskAssigneeDTO>> getAllTaskAssignees(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        List<TaskAssignee> assignees = taskAssigneeService.getAllAssigneesByTaskId(taskId, user);
        List<TaskAssigneeDTO> assigneeDTOs = assignees.stream()
                .map(TaskMapper::assigneeToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(assigneeDTOs);
    }
    
    /**
     * Retrieves all tasks assigned to a specific user.
     *
     * @param userId ID of the user whose tasks should be retrieved
     * @param requestUserId ID of the requesting user (for authorization)
     * @return ResponseEntity containing a list of task DTOs
     * @throws ApiException if either user is not found or requesting user lacks permissions
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TaskDTO>> getTasksByUserId(
            @PathVariable UUID userId,
            @RequestParam UUID requestUserId
    ) {
        User requestingUser = userRepository.findById(requestUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(requestingUser);
        
        List<Task> tasks = taskService.getTasksByUserId(userId, requestingUser);
        
        List<TaskDTO> taskDTOs = tasks.stream()
                .map(TaskMapper::taskToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(taskDTOs);
    }
    
    @GetMapping("/comments/user")
    public ResponseEntity<List<TaskCommentDTO>> getUserComments(
            @RequestParam UUID userId,
            @RequestParam UUID otherUserId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        List<TaskComment> comments = taskCommentService.getUserComments(user, otherUser);
        List<TaskCommentDTO> commentDTOs = comments.stream()
                .map(TaskMapper::commentToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commentDTOs);
    }
    
      /**
     * Retrieves all comments for a specific task.
     *
     * @param taskId ID of the task whose comments are to be retrieved
     * @param userId ID of the user requesting the comments
     * @return ResponseEntity containing a list of comments as DTOs
     * @throws ApiException if the user is not found, task doesn't exist, or user lacks access
     */
    @GetMapping("/{taskId}/comments")
    public ResponseEntity<List<TaskCommentDTO>> getTaskComments(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        List<TaskComment> comments = taskCommentService.getCommentsForTask(taskId, user);
        List<TaskCommentDTO> commentDTOs = comments.stream()
                .map(TaskMapper::commentToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commentDTOs);
    }

     /**
     * Deletes a specific comment from a task.
     *
     * @param taskId ID of the task containing the comment
     * @param commentId ID of the comment to delete
     * @param userId ID of the user performing the deletion
     * @return ResponseEntity with HTTP status 204 (NO CONTENT) on successful deletion
     * @throws ApiException if the user is not found, task/comment doesn't exist, or user lacks permissions
     */
    @DeleteMapping("/{taskId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        taskCommentService.deleteComment(taskId, user, commentId);
        return ResponseEntity.noContent().build();
    }

     /**
     * Deletes all comments from a specific task.
     *
     * @param taskId ID of the task whose comments are to be deleted
     * @param userId ID of the user performing the deletion
     * @return ResponseEntity with HTTP status 204 (NO CONTENT) on successful deletion
     * @throws ApiException if the user is not found, task doesn't exist, or user lacks permissions
     */
    @DeleteMapping("/{taskId}/comments")
    public ResponseEntity<Void> deleteAllCommentsInTask(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        taskCommentService.deleteAllCommentsForTask(taskId, user);
        return ResponseEntity.noContent().build();
    }

      /**
     * Adds a file to a task.
     *
     * @param taskId ID of the task to add the file to
     * @param taskFileDTO Data transfer object containing file details
     * @param userId ID of the user adding the file
     * @return ResponseEntity containing the created file as DTO with HTTP status 201 (CREATED)
     * @throws ApiException if the user is not found, task doesn't exist, or user lacks permissions
     */
    @PostMapping("/{taskId}/files")
    public ResponseEntity<TaskFileDTO> addFile(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskFileDTO taskFileDTO,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        TaskFile file = TaskMapper.dtoToFile(taskFileDTO);
        TaskFile createdFile = taskFileService.addTaskFile(taskId, user, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskMapper.fileToDTO(createdFile));

    }

    /**
     * Retrieves a specific file associated with a task.
     *
     * @param taskId ID of the task containing the file
     * @param fileId ID of the file to retrieve
     * @param userId ID of the user requesting the file
     * @return ResponseEntity containing the file as DTO
     * @throws ApiException if the user is not found, task/file doesn't exist, or user lacks access
     */
    @GetMapping("/{taskId}/files/{fileId}")
    public ResponseEntity<TaskFileDTO> getTaskFileByTaskId(
            @PathVariable UUID taskId,
            @PathVariable UUID fileId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        TaskFile file = taskFileService.getTaskFileById(taskId, fileId, user);
        return ResponseEntity.ok(TaskMapper.fileToDTO(file));
    }

     /**
     * Retrieves all files associated with a specific task.
     *
     * @param taskId ID of the task whose files are to be retrieved
     * @param userId ID of the user requesting the files
     * @return ResponseEntity containing a list of files as DTOs
     * @throws ApiException if the user is not found, task doesn't exist, or user lacks access
     */
    @GetMapping("/{taskId}/files")
    public ResponseEntity<List<TaskFileDTO>> getTaskFiles(
            @PathVariable UUID taskId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        List<TaskFile> files = taskFileService.getTaskFiles(taskId, user);
        List<TaskFileDTO> fileDTOs = files.stream()
                .map(TaskMapper::fileToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileDTOs);
    }
    
     /**
     * Retrieves all files associated with a specific task.
     *
     * @param taskId ID of the task whose files are to be retrieved
     * @param userId ID of the user requesting the files
     * @return ResponseEntity containing a list of files as DTOs
     * @throws ApiException if the user is not found, task doesn't exist, or user lacks access
     */
    @DeleteMapping("/{taskId}/files/{fileId}")
    public ResponseEntity<Void> deleteTaskFile(
            @PathVariable UUID taskId,
            @PathVariable UUID fileId,
            @RequestParam UUID userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(user);

        taskFileService.deleteTaskFile(taskId, fileId, user);
        return ResponseEntity.noContent().build();
    }


}

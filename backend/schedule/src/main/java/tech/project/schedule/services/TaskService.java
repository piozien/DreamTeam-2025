package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.enums.TaskStatus;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.utils.GetProjectRole;
import tech.project.schedule.services.utils.NotificationHelper;
import tech.project.schedule.utils.UserUtils;
import tech.project.schedule.repositories.TaskAssigneeRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import tech.project.schedule.repositories.ProjectRepository;
import tech.project.schedule.repositories.TaskRepository;

/**
 * Service for managing tasks within projects.
 * Handles CRUD operations for tasks with appropriate permission checks
 * and business rule validation.
 */
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final UserRepository userRepository;
    private final NotificationHelper notificationHelper;
    private final GoogleCalendarService calendarService;
    
    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    /**
     * Creates a new task within a project.
     * Validates that the user has permission in the project and that
     * task dates are valid relative to the project timeline.
     * 
     * @param task The task to create
     * @param user The user creating the task
     * @return The saved task entity
     */
    @Transactional
    public Task createTask(Task task, User user){

        Project project = projectRepository.findById(task.getProject().getId())
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));

        ProjectUserRole role = GetProjectRole.getProjectRole(user, project);
        if(role == null){
            throw new ApiException("You don't have permission to create tasks in this project", HttpStatus.FORBIDDEN);
        }
        if(task.getStartDate().toLocalDate().isBefore(project.getStartDate())){
            throw new ApiException("Task start date must be after project start date", HttpStatus.BAD_REQUEST);
        }
        task.setAssignees(new HashSet<>());
        task.setComments(new HashSet<>());
        task.setDependencies(new HashSet<>());
        task.setDependentTasks(new HashSet<>());
        task.setFiles(new HashSet<>());

        Task newTask = taskRepository.save(task);
        project.getTasks().add(newTask);
        projectRepository.save(project);
        
        // Notify project members that a new task has been created
        project.getMembers().values().forEach(member -> {
            notificationHelper.notifyUser(
                member.getUser(),
                NotificationStatus.TASK_UPDATED,
                "Utworzono nowe zadanie: " + task.getName() + " w projekcie " + project.getName()
            );
        });
        
        return newTask;
    }

    /**
     * Updates an existing task.
     * Only project managers and task assignees can update tasks.
     * Validates business rules like date constraints.
     * 
     * @param updatedTask Task with updated fields
     * @param taskId ID of the task to update
     * @param user User performing the update
     * @return The updated task entity
     */
    @Transactional
    public Task updateTask(UUID taskId, Task updatedTask, User user) {
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        
        // Check permissions
        ProjectUserRole role = GetProjectRole.getProjectRole(user, existingTask.getProject());
        boolean isAssignee = existingTask.getAssignees().stream()
                .anyMatch(assignee -> assignee.getUser().getId().equals(user.getId()));
        
        if (role != ProjectUserRole.PM && !isAssignee) {
            throw new ApiException("You don't have permission to update this task", HttpStatus.FORBIDDEN);
        }

        // Update basic task properties
        if (updatedTask.getName() != null) {
            existingTask.setName(updatedTask.getName());
        }
        if (updatedTask.getDescription() != null) {
            existingTask.setDescription(updatedTask.getDescription());
        }
        if (updatedTask.getStartDate() != null) {
            // Validate start date against project dates
            if (updatedTask.getStartDate().toLocalDate().isBefore(existingTask.getProject().getStartDate())) {
                throw new ApiException("Task start date cannot be before project start date", HttpStatus.BAD_REQUEST);
            }
            if (existingTask.getProject().getEndDate() != null && 
                updatedTask.getStartDate().toLocalDate().isAfter(existingTask.getProject().getEndDate())) {
                throw new ApiException("Task start date cannot be after project end date", HttpStatus.BAD_REQUEST);
            }
            existingTask.setStartDate(updatedTask.getStartDate());
        }
        if (updatedTask.getEndDate() != null) {
            LocalDateTime startDate = existingTask.getStartDate();
            if (updatedTask.getStartDate() != null) {
                startDate = updatedTask.getStartDate();
            }
            if (updatedTask.getEndDate().isBefore(startDate)) {
                throw new ApiException("End date cannot be before start date", HttpStatus.BAD_REQUEST);
            }
            // Validate end date against project dates
            if (existingTask.getProject().getEndDate() != null && 
                updatedTask.getEndDate().toLocalDate().isAfter(existingTask.getProject().getEndDate())) {
                throw new ApiException("Task end date cannot be after project end date", HttpStatus.BAD_REQUEST);
            }
            existingTask.setEndDate(updatedTask.getEndDate());
        }

        // Handle calendar events when task dates change
        boolean datesChanged = (updatedTask.getStartDate() != null || updatedTask.getEndDate() != null);
        if (datesChanged && existingTask.getCalendarEventId() != null) {
            try {
                // Update the main calendar event for this task using service account
                LocalDateTime startDateTime = existingTask.getStartDate();
                LocalDateTime endDateTime = existingTask.getEndDate() != null ? 
                    existingTask.getEndDate() :
                    existingTask.getStartDate();
                
                ZonedDateTime startZoned = startDateTime.atZone(ZoneId.of("Europe/Warsaw"));
                ZonedDateTime endZoned = endDateTime.atZone(ZoneId.of("Europe/Warsaw"));
                
                String summary = existingTask.getName() + " (" + existingTask.getProject().getName() + ")"; 
                
                // Get current event to preserve description with assignees
                calendarService.updateEventWithServiceAccount(
                    existingTask.getCalendarEventId(),
                    summary,
                    startZoned,
                    endZoned
                );
                
                log.info("Updated calendar event {} for task {}", existingTask.getCalendarEventId(), existingTask.getName());
            } catch (Exception e) {
                log.error("Failed to update calendar event: {}", e.getMessage());
            }
        }

        if (updatedTask.getStatus() != null) {
            boolean wasCompleted = existingTask.getStatus() == TaskStatus.FINISHED;
            boolean isNowCompleted = updatedTask.getStatus() == TaskStatus.FINISHED;
            
            if (!wasCompleted && isNowCompleted && existingTask.getEndDate() == null) {
                existingTask.setEndDate(LocalDateTime.now());
            }
            if (wasCompleted && !isNowCompleted && updatedTask.getEndDate() == null) {
                existingTask.setEndDate(null);
            }
            existingTask.setStatus(updatedTask.getStatus());
            
            // Handle calendar events when task is completed
            if (!wasCompleted && isNowCompleted) {
                // Remove calendar event for the task when it is completed
                if (existingTask.getCalendarEventId() != null) {
                    try {
                        calendarService.deleteEventWithServiceAccount(existingTask.getCalendarEventId());
                        existingTask.setCalendarEventId(null);
                        
                        // Clear calendar event IDs from all assignees
                        existingTask.getAssignees().forEach(assignee -> {
                            assignee.setCalendarEventId(null);
                            taskAssigneeRepository.save(assignee);
                        });
                    } catch (Exception e) {
                        log.error("Failed to delete calendar event: {}", e.getMessage());
                    }
                }
                
                // Notify all assignees
                existingTask.getAssignees().forEach(assignee -> {
                    notificationHelper.notifyTaskAssignee(
                        assignee.getUser(),
                        NotificationStatus.TASK_COMPLETED,
                        existingTask.getName()
                    );
                });
            }
        }
        
        Task savedTask = taskRepository.save(existingTask);
        
        // Notify assignees about task update
        existingTask.getAssignees().forEach(assignee -> {
            if (!assignee.getUser().getId().equals(user.getId())) {
                notificationHelper.notifyTaskAssignee(
                    assignee.getUser(),
                    NotificationStatus.TASK_UPDATED,
                    existingTask.getName()
                );
            }
        });
        
        return savedTask;
    }

    /**
     * Deletes a task.
     * Only project managers can delete tasks.
     * 
     * @param taskId ID of the task to delete
     * @param user User attempting to delete the task
     */
    @Transactional
    public void deleteTask(UUID taskId, User user){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isPM = GetProjectRole.getProjectRole(user, task.getProject()) == ProjectUserRole.PM;
        if(!isPM){
            throw new ApiException("You cannot delete this task", HttpStatus.FORBIDDEN);
        }
        
        // Before deleting, save the list of assigned users
        List<User> assignees = task.getAssignees().stream()
            .map(TaskAssignee::getUser)
            .collect(Collectors.toList());

        if (task.getCalendarEventId() != null && !task.getCalendarEventId().isEmpty()) {
            try {
                // Delete the event using service account
                calendarService.deleteEventWithServiceAccount(task.getCalendarEventId());
                log.info("Deleted calendar event {} for task {}", task.getCalendarEventId(), task.getName());
            } catch (Exception e) {
                log.error("Failed to delete main calendar event during task deletion: {}", e.getMessage());
            }
        }
        
        // Save the task name for use in notifications
        String taskName = task.getName();
        
        taskRepository.deleteById(taskId);
        
        // Notify assigned users of task deletion
        assignees.forEach(assignee -> {
            if (!assignee.getId().equals(user.getId())) {
                notificationHelper.notifyTaskAssignee(
                    assignee,
                    NotificationStatus.TASK_DELETED,
                    taskName
                );
            }
        });
    }

    /**
     * Retrieves a task by its ID.
     * Only project members and task assignees can view tasks.
     * 
     * @param taskId ID of the task to retrieve
     * @param user User requesting the task
     * @return The requested task entity
     */
    public Task getTaskById(UUID taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        
        ProjectUserRole role = GetProjectRole.getProjectRole(user, task.getProject());
        boolean isAssignee = task.getAssignees().stream()
                .anyMatch(assignee -> assignee.getUser().getId().equals(user.getId()));
        
        if (role == null && !isAssignee) {
            throw new ApiException("You don't have permission to view this task", HttpStatus.FORBIDDEN);
        }
        
        return task;
    }

    /**
     * Retrieves all tasks within a project.
     * Only project members can view project tasks.
     * 
     * @param projectId ID of the project
     * @param user User requesting the tasks
     * @return List of tasks in the project
     */
    public List<Task> getTasksByProject(UUID projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));
        
        ProjectUserRole role = GetProjectRole.getProjectRole(user, project);
        if (role == null) {
            throw new ApiException("You don't have permission to view tasks in this project", HttpStatus.FORBIDDEN);
        }
        
        return new ArrayList<>(project.getTasks());
    }
    
    /**
     * Retrieves all tasks assigned to a specific user.
     * This method finds all tasks that the user is assigned to across all projects.
     *
     * @param userId ID of the user whose tasks are to be retrieved
     * @param requestingUser The user making the request (for authorization)
     * @return List of tasks assigned to the specified user
     * @throws ApiException if the user is not found or requesting user lacks authorization
     */
    public List<Task> getTasksByUserId(UUID userId, User requestingUser) {
        // Ensure the requesting user is authorized
        UserUtils.assertAuthorized(requestingUser);
        
        // Check if the user exists
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (!requestingUser.getId().equals(userId) && requestingUser.getGlobalRole() != GlobalRole.ADMIN) {
            throw new ApiException("You don't have permission to view tasks for this user", HttpStatus.FORBIDDEN);
        }
        
        // Get all task assignments for the user
        List<TaskAssignee> assignments = taskAssigneeRepository.findAllByUser_Id(userId);
        
        // Extract tasks from assignments
        List<Task> tasks = assignments.stream()
                .map(TaskAssignee::getTask)
                .distinct()
                .collect(Collectors.toList());
        
        return tasks;
    }
    
    /**
     * Gets an admin user ID for calendar operations
     * 
     * @param project The project to get admin for
     * @return User ID of a project manager or system admin
     */
    private UUID getAdminUserId(Project project) {
        //First, we are looking for a Project Manager on the project
        for (ProjectMember member : project.getMembers().values()) {
            if (member.getRole() == ProjectUserRole.PM) {
                return member.getUser().getId();
            }
        }
        
        // If there is no PM, look for a user with the ADMIN role among the project members
        for (ProjectMember member : project.getMembers().values()) {
            if (member.getUser().getGlobalRole() == GlobalRole.ADMIN) {
                return member.getUser().getId();
            }
        }

        return null;
    }
}

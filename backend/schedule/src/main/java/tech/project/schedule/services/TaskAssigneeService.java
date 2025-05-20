package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskAssigneeRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.services.utils.GetProjectRole;
import tech.project.schedule.services.utils.NotificationHelper;


import com.google.api.services.calendar.model.Event;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

/**
 * Service responsible for managing task assignments to users.
 * Handles the creation and removal of task-user associations with
 * appropriate permission checks.
 */
@Service
@RequiredArgsConstructor
public class TaskAssigneeService {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final NotificationHelper notificationHelper;
    private final GoogleCalendarService calendarService;

    /**
     * Creates a calendar event for a task.
     * If the task doesn't have an associated event yet, a new one is created.
     * Otherwise, the user is added as an attendee to the existing event.
     * 
     * @param task The task to create an event for
     * @param user The user to create the event for or add as attendee
     * @return The ID of the created or updated calendar event
     */
    private String createOrUpdateCalendarEvent(Task task, User user) {
        try {
            // Get project admin or system admin user ID to manage the calendar
            UUID adminUserId = getAdminUserId(task.getProject());
            if (adminUserId == null) {
                // Fallback to current user if no admin found
                adminUserId = user.getId(); 
            }
            
            // Convert task dates to ZonedDateTime
            LocalDateTime startDateTime = task.getStartDate();
            LocalDateTime endDateTime = task.getEndDate() != null ? 
                task.getEndDate():
                task.getStartDate().plusHours(8);
                
            ZonedDateTime startZoned = startDateTime.atZone(ZoneId.of("Europe/Warsaw"));
            ZonedDateTime endZoned = endDateTime.atZone(ZoneId.of("Europe/Warsaw"));
            
            // Check if task already has an event
            if (task.getCalendarEventId() == null || task.getCalendarEventId().isEmpty()) {
                // Create new event for the task
                String eventSummary = task.getName() + " (" + task.getProject().getName() + ")"; 
                Event event = calendarService.createTaskEvent(
                    adminUserId, 
                    eventSummary,
                    startZoned,
                    endZoned,
                    user.getEmail()
                );
                
                // Save event ID to task
                task.setCalendarEventId(event.getId());
                return event.getId();
            } else {
                // Add user as attendee to existing event
                Event updatedEvent = calendarService.addAttendeeToEvent(
                    adminUserId,
                    task.getCalendarEventId(),
                    user.getEmail()
                );
                return updatedEvent.getId();
            }
        } catch (Exception e) {
            // Log error but don't fail the assignment
            System.err.println("Failed to manage calendar event: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets an admin user ID for calendar operations
     * 
     * @param project The project to get admin for
     * @return User ID of a project manager or system admin
     */
    private UUID getAdminUserId(Project project) {
        for (ProjectMember member : project.getMembers().values()) {
            if (member.getRole() == ProjectUserRole.PM) {
                return member.getUser().getId();
            }
        }
        return null; // No admin found
    }

    /**
     * Assigns a project member to a specific task.
     * Only Project Managers can assign users to tasks.
     * Creates a calendar event for the assigned user.
     * 
     * @param taskId ID of the task to assign a user to
     * @param user The user performing the assignment (must be a PM)
     * @param userToBeAdded The user to assign to the task
     * @return The created assignment relationship
     */
    @Transactional
    public TaskAssignee assignMemberToTask(UUID taskId, User user, User userToBeAdded){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isPM = GetProjectRole.getProjectRole(user, task.getProject()) == ProjectUserRole.PM;
        if(!isPM){
            throw new ApiException("You dont have permission to add assignees to this task.", HttpStatus.FORBIDDEN);
        }
        
        ProjectUserRole userRole = GetProjectRole.getProjectRole(userToBeAdded, task.getProject());
        if (userRole == null) {
            throw new ApiException("User is not a member of this project", HttpStatus.BAD_REQUEST);
        }

        boolean isAlreadyAssigned = task.getAssignees().stream()
                .anyMatch(assignee -> assignee.getUser().getId().equals(userToBeAdded.getId()));
                
        if(isAlreadyAssigned){
            throw new ApiException("This member is already assigned to this task", HttpStatus.CONFLICT);
        }

        TaskAssignee newAssignee = new TaskAssignee();
        newAssignee.setTask(task);
        newAssignee.setUser(userToBeAdded);
        
        // Create or update calendar event for the task and add the user as attendee
        try {
            String eventId = createOrUpdateCalendarEvent(task, userToBeAdded);
            if (eventId != null) {
                newAssignee.setCalendarEventId(eventId);
                // If this is the first assignment, save the event ID to the task
                if (task.getCalendarEventId() == null || task.getCalendarEventId().isEmpty()) {
                    task.setCalendarEventId(eventId);
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the assignment if calendar integration fails
            System.err.println("Failed to create/update calendar event: " + e.getMessage());
        }
        
        taskRepository.save(task);
        TaskAssignee savedAssignee = taskAssigneeRepository.save(newAssignee);
        
        // Notify the user adding the assignment
        notificationHelper.notifyUser(
            user,
            NotificationStatus.TASK_ASSIGNEE_ADDED,
            "Pomyślnie dodano użytkownika " + userToBeAdded.getName() + " do zadania " + task.getName()
        );

        //Notify the added user
        notificationHelper.notifyTaskAssignee(
            userToBeAdded,
            NotificationStatus.TASK_ASSIGNEE_ADDED,
            task.getName()
        );
        
        return savedAssignee;
    }

    /**
     * Removes a user assignment from a task.
     * Only Project Managers can remove assignments.
     * Removes the user as an attendee from the associated calendar event.
     */
    @Transactional
    public void removeAssigneeFromTask(UUID taskId, UUID assigneeId, User currentUser) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        boolean isPM = GetProjectRole.getProjectRole(currentUser, task.getProject()) == ProjectUserRole.PM;
        if (!isPM) {
            throw new ApiException("You don't have permission to remove assignees from this task", HttpStatus.FORBIDDEN);
        }
        
        TaskAssignee assigneeToRemove = task.getAssignees().stream()
                .filter(assignee -> assignee.getId().equals(assigneeId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Assignee not found", HttpStatus.NOT_FOUND));
        
        // Remove user as attendee from calendar event
        if (task.getCalendarEventId() != null) {
            try {
                // Get an admin to perform the calendar operation
                UUID adminUserId = getAdminUserId(task.getProject());
                if (adminUserId == null) {
                    adminUserId = currentUser.getId(); // Fallback to current user
                }
                
                calendarService.removeAttendeeFromEvent(
                    adminUserId,
                    task.getCalendarEventId(),
                    assigneeToRemove.getUser().getEmail()
                );
            } catch (Exception e) {
                // Log error but don't fail the removal if calendar integration fails
                System.err.println("Failed to remove user from calendar event: " + e.getMessage());
            }
        }

        User userToNotify = assigneeToRemove.getUser();
        
        task.getAssignees().remove(assigneeToRemove);
        taskRepository.save(task);
        taskAssigneeRepository.delete(assigneeToRemove);
        
        // Notify the deleted user
        notificationHelper.notifyUser(
            userToNotify,
            NotificationStatus.TASK_UPDATED,
            "Zostałeś usunięty z zadania " + task.getName()
        );
    }

    /**
     * Retrieves all task assignments for a specific task.
     * Only Project Managers and system Admins can access the full list.
     */
    public List<TaskAssignee> getAllAssigneesByTaskId(UUID taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        ProjectUserRole role = GetProjectRole.getProjectRole(user, task.getProject());
        boolean isPM = role == ProjectUserRole.PM;
        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
        
        if (!isPM && !isAdmin) {
            throw new ApiException("Only Project Managers and Admins can view all assignees", HttpStatus.FORBIDDEN);
        }
        
        return taskAssigneeRepository.findAllByTask_Id(taskId);
    }

    /**
     * Gets task assignments with more permissive access controls.
     * Project members and assignees can view the task's assignment list.
     */
    public Set<TaskAssignee> getTaskAssignees(UUID taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        ProjectUserRole role = GetProjectRole.getProjectRole(user, task.getProject());
        boolean isAssignee = task.getAssignees().stream()
                .anyMatch(assignee -> assignee.getUser().getId().equals(user.getId()));
                
        if (role == null && !isAssignee) {
            throw new ApiException("You don't have permission to view assignees for this task", HttpStatus.FORBIDDEN);
        }
        
        return new HashSet<>(task.getAssignees());
    }
}

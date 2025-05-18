package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskAssigneeRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.services.utils.GetProjectRole;
import tech.project.schedule.services.utils.NotificationHelper;
import tech.project.schedule.dto.calendar.EventDTO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * Creates a calendar event for a task assignment.
     * 
     * @param task The task to create an event for
     * @param user The user to create the event for
     * @return The ID of the created calendar event
     */
    private String createCalendarEvent(Task task, User user) {
        // Convert task dates to datetime format
        LocalDateTime startDateTime = task.getStartDate();
        LocalDateTime endDateTime = task.getEndDate() != null ? 
            task.getEndDate():
            task.getStartDate().plusHours(8);

        // Format dates for Google Calendar
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm:ss");
        String startDateTimeStr = startDateTime.format(formatter);
        String endDateTimeStr = endDateTime.format(formatter);

        // Create event DTO
        EventDTO eventDTO = new EventDTO(
            task.getName(),
            "Task in project: " + task.getProject().getName() + "\n" + task.getDescription(),
            startDateTimeStr,
            endDateTimeStr,
            "Europe/Warsaw"
        );

        // Create calendar event only for the assigned user
        return calendarService.createEvent(user.getId(), eventDTO);
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
        taskRepository.save(task);
        
        TaskAssignee savedAssignee = taskAssigneeRepository.save(newAssignee);
        
        // Create calendar event for the assigned user
        try {
            String eventId = createCalendarEvent(task, userToBeAdded);
            savedAssignee.setCalendarEventId(eventId);
            taskAssigneeRepository.save(savedAssignee);
        } catch (Exception e) {
            // Log error but don't fail the assignment if calendar integration fails
            System.err.println("Failed to create calendar event: " + e.getMessage());
        }
        
        // Powiadom użytkownika dodającego przypisanie
        notificationHelper.notifyUser(
            user,
            NotificationStatus.TASK_ASSIGNEE_ADDED,
            "Pomyślnie dodano użytkownika " + userToBeAdded.getName() + " do zadania " + task.getName()
        );
        
        // Powiadom dodanego użytkownika
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
     * Removes the associated calendar event.
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
        
        // Remove calendar event if it exists
        if (assigneeToRemove.getCalendarEventId() != null) {
            try {
                calendarService.deleteEvent(assigneeToRemove.getUser().getId(), assigneeToRemove.getCalendarEventId());
            } catch (Exception e) {
                // Log error but don't fail the removal if calendar integration fails
                System.err.println("Failed to delete calendar event: " + e.getMessage());
            }
        }
        
        // Zapisz referencję do usuwanego użytkownika
        User userToNotify = assigneeToRemove.getUser();
        
        task.getAssignees().remove(assigneeToRemove);
        taskRepository.save(task);
        taskAssigneeRepository.delete(assigneeToRemove);
        
        // Powiadom usuniętego użytkownika
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

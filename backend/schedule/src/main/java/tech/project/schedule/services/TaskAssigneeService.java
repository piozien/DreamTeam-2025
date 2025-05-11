package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskAssigneeRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.services.utils.GetProjectRole;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class TaskAssigneeService {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final NotificationService notificationService;

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
        notificationService.sendNotificationToUser(
                user,
                NotificationStatus.TASK_ASSIGNEE_ADDED,
                "You have successfully added user "+userToBeAdded.getName()+" to the task "+task.getName()
        );
        notificationService.sendNotificationToUser(
                userToBeAdded,
                NotificationStatus.TASK_ASSIGNEE_ADDED,
                "You have been added to the task "+task.getName()
        );
        return savedAssignee;
    }

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
        
        task.getAssignees().remove(assigneeToRemove);
        taskRepository.save(task);
        taskAssigneeRepository.delete(assigneeToRemove);
    }

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

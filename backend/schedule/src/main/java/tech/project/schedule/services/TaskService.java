package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.hibernate.jdbc.Expectation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.enums.TaskStatus;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.user.User;
import tech.project.schedule.services.utils.GetProjectRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import tech.project.schedule.repositories.ProjectRepository;
import tech.project.schedule.repositories.TaskRepository;



@Service
@RequiredArgsConstructor
public class TaskService {
    private final NotificationService notificationService;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public Task createTask(Task task, User user){

        Project project = projectRepository.findById(task.getProject().getId())
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));

        ProjectUserRole role = GetProjectRole.getProjectRole(user, project);
        if(role == null){
            throw new ApiException("You don't have permission to create tasks in this project", HttpStatus.FORBIDDEN);
        }
        if(task.getStartDate().isBefore(project.getStartDate())){
            throw new ApiException("Task start date must be after project start date", HttpStatus.BAD_REQUEST);
        }
        task.setAssignees(new HashSet<>());
        task.setComments(new HashSet<>());
        task.setHistory(new HashSet<>());
        task.setDependencies(new HashSet<>());
        task.setDependentTasks(new HashSet<>());
        task.setFiles(new HashSet<>());

        Task newTask = taskRepository.save(task);
        project.getTasks().add(newTask);
        projectRepository.save(project);
        return newTask;
    }

    @Transactional
    public Task updateTask(Task updatedTask, UUID taskId, User user) {

        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        ProjectUserRole role = GetProjectRole.getProjectRole(user, existingTask.getProject());
        boolean isAssignee = existingTask.getAssignees().stream()
                .anyMatch(assignee -> assignee.getUser().getId().equals(user.getId()));

        if (role != ProjectUserRole.PM && !isAssignee) {
            throw new ApiException("You don't have permission to update this task", HttpStatus.FORBIDDEN);
        }

        if (updatedTask.getName() != null && !updatedTask.getName().equals(existingTask.getName()) && taskRepository.existsByName(updatedTask.getName())) {
            throw new ApiException("Task by name: " + updatedTask.getName() + " already exists.", HttpStatus.CONFLICT);
        }
        if (updatedTask.getName() != null) {
            existingTask.setName(updatedTask.getName());
        }
        if (updatedTask.getDescription() != null) {
            existingTask.setDescription(updatedTask.getDescription());
        }
        if (updatedTask.getStartDate() != null) {
            if (updatedTask.getStartDate().isBefore(existingTask.getProject().getStartDate())) {
                throw new ApiException("Task start date must be after project start date", HttpStatus.BAD_REQUEST);
            }
            existingTask.setStartDate(updatedTask.getStartDate());
        }
        if (updatedTask.getPriority() != null) {
            existingTask.setPriority(updatedTask.getPriority());
        }
        if (updatedTask.getStatus() != null) {
            boolean wasCompleted = existingTask.getStatus() == TaskStatus.FINISHED;
            boolean isNowCompleted = updatedTask.getStatus() == TaskStatus.FINISHED;
            if (!wasCompleted && isNowCompleted) {
                existingTask.setEndDate(LocalDate.now());
            }
            if (wasCompleted && !isNowCompleted) {
                existingTask.setEndDate(null);
            }
            existingTask.setStatus(updatedTask.getStatus());
        }
        var saved = taskRepository.save(existingTask);
        NotificationStatus statusNot = existingTask.getStatus() == TaskStatus.FINISHED  ? NotificationStatus.TASK_COMPLETED : NotificationStatus.TASK_UPDATED;
        for(TaskAssignee assignee :  existingTask.getAssignees()) {
            notificationService.sendNotification(
                    assignee.getUser().getId(),
                    Notification.builder()
                            .user(assignee.getUser())
                            .status(statusNot)
                            .message("Task " + existingTask.getName() + " has been updated.")
                            .build()
            );
        }
        return saved;
    }

    @Transactional
    public void deleteTask(UUID taskId, User user){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isPM = GetProjectRole.getProjectRole(user, task.getProject()) == ProjectUserRole.PM;
        if(!isPM){
            throw new ApiException("You cannot delete this task", HttpStatus.FORBIDDEN);
        }
        taskRepository.deleteById(taskId);
    }
    
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
    
    public List<Task> getTasksByProject(UUID projectId, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));
        
        ProjectUserRole role = GetProjectRole.getProjectRole(user, project);
        if (role == null) {
            throw new ApiException("You don't have permission to view tasks in this project", HttpStatus.FORBIDDEN);
        }
        
        return new ArrayList<>(project.getTasks());
    }
}

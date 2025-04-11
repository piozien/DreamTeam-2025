package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.enums.TaskStatus;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.project.ProjectMember;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskDependency;
import tech.project.schedule.model.task.TaskFile;
import tech.project.schedule.model.user.User;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;

import tech.project.schedule.repositories.ProjectRepository;
import tech.project.schedule.repositories.TaskRepository;



@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public Task createTask(Task task, User user){
        Project project = projectRepository.findById(task.getProject().getId())
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND));

        ProjectUserRole role = getProjectRole(user, project);
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

    public Task updateTask(Task updatedTask, UUID taskId, User user) {

        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        ProjectUserRole role = getProjectRole(user, existingTask.getProject());
        boolean isAssignee = existingTask.getAssignees().stream()
                .anyMatch(assignee -> assignee.getUser().equals(user.getId()));

        if(role != ProjectUserRole.PM && !isAssignee){
            throw new ApiException("You don't have permission to update this task", HttpStatus.FORBIDDEN);
        }

        if(!taskRepository.existsByName(updatedTask.getName())){
            throw new ApiException("Task by name: "+ updatedTask.getName() +" already exists.", HttpStatus.NOT_FOUND);
        }
        if(updatedTask.getName() != null) {
            existingTask.setName(updatedTask.getName());
        }
        if(updatedTask.getDescription() != null) {
            existingTask.setDescription(updatedTask.getDescription());
        }
        if(updatedTask.getStartDate() != null) {
            if(updatedTask.getStartDate().isBefore(existingTask.getProject().getStartDate())){
                throw new ApiException("Task start date must be after project start date", HttpStatus.BAD_REQUEST);
            }
            existingTask.setStartDate(updatedTask.getStartDate());
        }
        if(updatedTask.getPriority() != null) {
            existingTask.setPriority(updatedTask.getPriority());
        }
        if(updatedTask.getStatus() != null) {
            boolean wasCompleted = existingTask.getStatus() == TaskStatus.FINISHED;
            boolean isNowCompleted = updatedTask.getStatus() == TaskStatus.FINISHED;
            if(!wasCompleted && isNowCompleted){
                existingTask.setEndDate(LocalDate.now());
            }
            if(wasCompleted && !isNowCompleted){
                existingTask.setEndDate(null);
            }
        }

        return taskRepository.save(existingTask);
    }




    public ProjectUserRole getProjectRole(User user, Project project) {
        if(user.getGlobalRole() == GlobalRole.ADMIN){
            return ProjectUserRole.PM;
        }
        ProjectMember member = project.getMembers().get(user.getId());
        return member != null ? member.getRole() : null;
    }
}

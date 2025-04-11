package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskAssigneeRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.services.utils.GetProjectRole;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskAssigneeService {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;

    @Transactional
    public TaskAssignee assignMemberToTask(UUID taskId, User user, User userToBeAdded){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isPM = GetProjectRole.getProjectRole(user, task.getProject()) == ProjectUserRole.PM;
        if(!isPM){
            throw new ApiException("You dont have permission to add assignees to this task.", HttpStatus.FORBIDDEN);
        }
        // ToDo: Fix the assignee to be added
        if(task.getAssignees().contains(userToBeAdded)){
            throw new ApiException("This member is already assigned to this task", HttpStatus.FORBIDDEN);
        }

        TaskAssignee newAssignee = new TaskAssignee();
        newAssignee.setTask(task);
        newAssignee.setUser(userToBeAdded);
        task.getAssignees().add(newAssignee);
        taskRepository.save(task);

        return newAssignee;
    }

    @Transactional
    public TaskAssignee removeMemberFromTask(UUID taskId, User user, TaskAssignee assigneeToBeRemoved){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isPM = GetProjectRole.getProjectRole(user, task.getProject()) == ProjectUserRole.PM;
        if(!isPM){
            throw new ApiException("You dont have permission to remove assignees from this task.", HttpStatus.FORBIDDEN);
        }
        if(!task.getAssignees().contains(assigneeToBeRemoved)){
            throw new ApiException("This member is not a part of this task.");
        }


        task.getAssignees().remove(assigneeToBeRemoved);
        taskRepository.save(task);

        return assigneeToBeRemoved;
    }

    public List<TaskAssignee> getAllAssigneesByTaskId(UUID taskId) {
        return taskAssigneeRepository.findAllByTask_Id(taskId);
    }

    public List<TaskAssignee> getAllAssigneesByUserId(UUID userId) {
        return taskAssigneeRepository.findAllByUser_Id(userId);
    }

}

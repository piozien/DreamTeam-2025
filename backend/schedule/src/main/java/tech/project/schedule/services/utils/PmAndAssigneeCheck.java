package tech.project.schedule.services.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskRepository;

import java.util.UUID;

@Component
public class PmAndAssigneeCheck {
    private static TaskRepository taskRepository;
    
    @Autowired
    public void setTaskRepository(TaskRepository taskRepository) {
        PmAndAssigneeCheck.taskRepository = taskRepository;
    }

    private static Task giveMeTask(UUID taskId){
    
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task with ID " + taskId + " not found", HttpStatus.NOT_FOUND));
    }

    public static boolean checkIfNotPmAndAssignee(UUID taskId, User user){
        boolean isPM = GetProjectRole.getProjectRole(user, giveMeTask(taskId).getProject()) == ProjectUserRole.PM;
        boolean isAssignee = GetTaskAssignee.getAssignee(giveMeTask(taskId), user) == user;
        return !(isPM || isAssignee);
    }
}

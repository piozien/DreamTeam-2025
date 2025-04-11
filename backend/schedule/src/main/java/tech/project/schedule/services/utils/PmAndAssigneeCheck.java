package tech.project.schedule.services.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskRepository;

import java.util.UUID;

@RequiredArgsConstructor
public class PmAndAssigneeCheck {
    private static TaskRepository taskRepository;

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

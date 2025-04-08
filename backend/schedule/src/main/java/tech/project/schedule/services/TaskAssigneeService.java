package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskAssigneeRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.repositories.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskAssigneeService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskAssigneeRepository taskAssigneeRepository;

    public void assignUserToTask(UUID taskId, UUID userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        TaskAssignee taskAssignee = new TaskAssignee();
        taskAssignee.setTask(task);
        taskAssignee.setUser(user);
        taskAssigneeRepository.save(taskAssignee);
    }

    public void removeUserFromTask(UUID taskId, UUID userId) {
        taskAssigneeRepository.deleteByTask_IdAndUser_Id(taskId, userId);
    }

    public List<TaskAssignee> getAllAssigneesByTaskId(UUID taskId) {
        return taskAssigneeRepository.findAllByTask_Id(taskId);
    }

    public List<TaskAssignee> getAllAssigneesByUserId(UUID userId) {
        return taskAssigneeRepository.findAllByUser_Id(userId);
    }

}

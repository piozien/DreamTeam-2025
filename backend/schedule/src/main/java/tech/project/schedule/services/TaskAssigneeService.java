package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.project.schedule.model.task.TaskAssignee;
import tech.project.schedule.repositories.TaskRepository;

@Service
@RequiredArgsConstructor
public class TaskAssigneeService {
    private final TaskRepository taskRepository;

    public TaskAssignee assignTask(TaskAssignee taskAssignee) {
        return taskAssignee;
    }
}

package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.task.TaskFile;
import tech.project.schedule.repositories.TaskFileRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskFileService {

    private final TaskFileRepository taskFileRepository;

    public TaskFile getTaskFileByTaskId(UUID taskId) {
        return taskFileRepository.findByTask_Id(taskId);
    }

    public TaskFile getTaskFileByPath(String filePath) {
        return taskFileRepository.findByFilePath(filePath);
    }

    public void updateFilePathByTaskId(UUID taskId, String newFilePath) {
        if (!taskFileRepository.existsById(taskId)) {
            throw new ApiException("TaskFile with task ID " + taskId + " not found");
        }
        taskFileRepository.setFilePath(taskId, newFilePath);
    }

    public boolean doesFileExist(String filePath) {
        return taskFileRepository.existsByFilePath(filePath);
    }

    public void deleteTaskFileByPath(String filePath) {
        TaskFile taskFile = taskFileRepository.findByFilePath(filePath);
        if (taskFile == null) {
            throw new ApiException("File with path " + filePath + " not found");
        }
        taskFileRepository.delete(taskFile);
    }

    public void deleteTaskFileById(UUID taskFileId) {
        if (!taskFileRepository.existsById(taskFileId)) {
            throw new ApiException("TaskFile with ID " + taskFileId + " not found");
        }
        taskFileRepository.deleteById(taskFileId);
    }
}

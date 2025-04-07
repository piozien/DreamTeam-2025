package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.task.TaskFile;
import tech.project.schedule.repositories.TaskFileRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskFileService{
    private final TaskFileRepository taskFileRepository;

    public TaskFile getTaskFileByTaskId(UUID taskId) {
        return taskFileRepository.findByTaskId(taskId);
    }

    public TaskFile getTaskFileByPath(String filePath) {            //same as above
        return taskFileRepository.findByFilePath(filePath);
    }

    public TaskFile updateFilePath(UUID taskFileId, String newFilePath)
    {
        TaskFile taskFile = taskFileRepository.findById(taskFileId)
                .orElseThrow(() -> new ApiException("File with ID " + taskFileId + " not found"));
        taskFile.setFilePath(newFilePath);
        return taskFileRepository.save(taskFile);
    }

    public boolean doesFileExist(String filePath) {
        return taskFileRepository.existsByFilePath(filePath);
    }

    public void DeleteTaskFile(String filePath){
        TaskFile taskfile = taskFileRepository.findByFilePath(filePath);
        if(taskfile != null){
            throw new ApiException("File with path " + filePath + " not found");
        }
        taskFileRepository.delete(taskfile);
    }

    public void deleteTaskFileById(UUID taskFileId) {       //same thing as above but with ID
        if (!taskFileRepository.existsById(taskFileId)) {
            throw new ApiException("TaskFile with ID " + taskFileId + " not found");
        }

        taskFileRepository.deleteById(taskFileId);
    }
}
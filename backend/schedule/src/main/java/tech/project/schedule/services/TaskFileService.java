package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.task.TaskFile;
import tech.project.schedule.repositories.TaskFileRepository;
import tech.project.schedule.services.utils.GetProjectRole;
import tech.project.schedule.services.utils.GetTaskAssignee;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.model.user.User;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.services.utils.PmAndAssigneeCheck;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskFileService {

    private final TaskFileRepository taskFileRepository;
    private final TaskRepository taskRepository;

    public TaskFile addTaskFile(UUID taskId, User user, TaskFile file) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId,user)){
            throw new ApiException("You dont have permission to add files");
        }
        if(file.getFilePath() == null){
            throw new ApiException("File path is null", HttpStatus.BAD_REQUEST);
        }
        taskFileRepository.setFilePath(taskId, file.getFilePath());
        task.getFiles().add(file);
        taskRepository.save(task);
        return file;
    }

    public void updateFilePathByTaskId(UUID taskId, String newFilePath, User user) {
        if (!taskFileRepository.existsById(taskId)) {
            throw new ApiException("TaskFile with task ID " + taskId + " not found", HttpStatus.NOT_FOUND);
        }
        if (PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId,user)) {
            throw new ApiException("You dont have permission to update files on this task");
        }
        taskFileRepository.setFilePath(taskId, newFilePath);
    }

    public boolean doesFileExist(String filePath) {
        return taskFileRepository.existsByFilePath(filePath);
    }

    public void deleteTaskFileByPath(String filePath, UUID taskId, User user) {
        TaskFile taskFile = taskFileRepository.findByFilePath(filePath);
        if(!doesFileExist(taskFile.getFilePath())){
            throw new ApiException("TaskFile with task ID " + taskId + " not found", HttpStatus.NOT_FOUND);
        }
        if (PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId,user)) {
            throw new ApiException("You dont have permission to delete files on this task");
        }
        taskFileRepository.delete(taskFile);
    }

    public void deleteTaskFileById(UUID taskFileId, UUID taskId, User user) {
        if (!taskFileRepository.existsById(taskFileId)) {
            throw new ApiException("TaskFile with ID " + taskFileId + " not found", HttpStatus.NOT_FOUND);
        }
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId,user)) {
            throw new ApiException("You dont have permission to delete files on this task");
        }
        taskFileRepository.deleteById(taskFileId);
    }

    public TaskFile getTaskFileByTaskId(UUID taskId, User user) {
        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isInProject = task.getProject().getMembers().containsKey(user.getId());
        if(!isAdmin && !isInProject){
            throw new ApiException("You dont have permission to view files in this task", HttpStatus.FORBIDDEN);
        }
        return taskFileRepository.findByTask_Id(taskId);
    }

    public TaskFile getTaskFileByPath(String filePath) {
        return taskFileRepository.findByFilePath(filePath);
    }
}
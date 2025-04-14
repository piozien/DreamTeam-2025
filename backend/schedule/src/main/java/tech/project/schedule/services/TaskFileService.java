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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskFileService {

    private final TaskFileRepository taskFileRepository;
    private final TaskRepository taskRepository;

    public TaskFile addTaskFile(UUID taskId, User user, TaskFile file) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)){
            throw new ApiException("You don't have permission to add files", HttpStatus.FORBIDDEN);
        }
        
        if(file.getFilePath() == null){
            throw new ApiException("File path is null", HttpStatus.BAD_REQUEST);
        }
        
        file.setTask(task);
        file.setUploadedBy(user);
        
        TaskFile savedFile = taskFileRepository.save(file);
        
        if (task.getFiles() == null) {
            task.setFiles(new java.util.HashSet<>());
        }
        task.getFiles().add(savedFile);
        taskRepository.save(task);
        
        return savedFile;
    }

    public void updateFilePathByTaskId(UUID taskId, String newFilePath, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        if (PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)) {
            throw new ApiException("You don't have permission to update files on this task", HttpStatus.FORBIDDEN);
        }
        
        taskFileRepository.setFilePathByTaskId(taskId, newFilePath);
    }
    
    public void updateFilePath(UUID taskId, UUID fileId, String newFilePath, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        TaskFile file = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
                
        if (!file.getTask().getId().equals(taskId)) {
            throw new ApiException("File does not belong to this task", HttpStatus.BAD_REQUEST);
        }
        
        if (PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)) {
            throw new ApiException("You don't have permission to update files on this task", HttpStatus.FORBIDDEN);
        }
        
        taskFileRepository.setFilePathById(fileId, newFilePath);
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

    public void deleteTaskFile(UUID taskId, UUID fileId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        TaskFile file = taskFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
                
        if (!file.getTask().getId().equals(taskId)) {
            throw new ApiException("File does not belong to this task", HttpStatus.BAD_REQUEST);
        }
        
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)) {
            throw new ApiException("You don't have permission to delete files on this task", HttpStatus.FORBIDDEN);
        }
        
        task.getFiles().remove(file);
        taskRepository.save(task);
        
        taskFileRepository.deleteById(fileId);
    }

    public List<TaskFile> getTaskFiles(UUID taskId, User user) {
        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isInProject = task.getProject().getMembers().containsKey(user.getId());
        
        if(!isAdmin && !isInProject){
            throw new ApiException("You don't have permission to view files in this task", HttpStatus.FORBIDDEN);
        }
        
        return taskFileRepository.findAllByTaskId(taskId);
    }
    
    public TaskFile getTaskFileById(UUID taskId, UUID fileId, User user) {
        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isInProject = task.getProject().getMembers().containsKey(user.getId());
        
        if(!isAdmin && !isInProject){
            throw new ApiException("You don't have permission to view files in this task", HttpStatus.FORBIDDEN);
        }
        
        return taskFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found", HttpStatus.NOT_FOUND));
    }

    public TaskFile getTaskFileByPath(String filePath, User user) {
        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
        if(filePath == null){
            throw new ApiException("File path cannot be empty", HttpStatus.BAD_REQUEST);
        }
        TaskFile file = taskFileRepository.findByFilePath(filePath);
        if(!doesFileExist(filePath)){
            throw new ApiException("File with file path: " + filePath + " does not exist.", HttpStatus.NOT_FOUND);
        }

        if(!isAdmin){
            throw new ApiException("You don't have permission to view files by path", HttpStatus.FORBIDDEN);
        }
        return file;
    }
}
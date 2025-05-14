package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.task.TaskFile;
import tech.project.schedule.repositories.TaskFileRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.model.user.User;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.services.utils.PmAndAssigneeCheck;

import java.util.List;
import java.util.UUID;

/**
 * Service class for managing file attachments to tasks in the scheduling system.
 * Provides functionality for uploading, retrieving, updating, and deleting files
 * associated with tasks, with appropriate permission checks based on user roles
 * and task assignments.
 */
@Service
@RequiredArgsConstructor
public class TaskFileService {

    private final TaskFileRepository taskFileRepository;
    private final TaskRepository taskRepository;

    /**
     * Attaches a new file to a task.
     * Only project managers and users assigned to the task can add files.
     *
     * @param taskId The ID of the task to attach the file to
     * @param user The user uploading the file
     * @param file The TaskFile entity with file metadata
     * @return The saved TaskFile entity
     * @throws ApiException if task not found, user lacks permission, or file path is null
     */
    @Transactional
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
        savedFile = taskFileRepository.findById(savedFile.getId()).orElseThrow(() -> new ApiException("Failed to save file", HttpStatus.INTERNAL_SERVER_ERROR));
        
        if (task.getFiles() == null) {
            task.setFiles(new java.util.HashSet<>());
        }
        task.getFiles().add(savedFile);
        taskRepository.save(task);
        
        return savedFile;
    }

    /**
     * Updates the file path for all files associated with a specific task.
     * Only project managers and users assigned to the task can update files.
     *
     * @param taskId The ID of the task
     * @param newFilePath The new file path to set
     * @param user The user performing the update
     * @throws ApiException if task not found or user lacks permission
     */
    @Transactional
    public void updateFilePathByTaskId(UUID taskId, String newFilePath, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
                
        if (PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)) {
            throw new ApiException("You don't have permission to update files on this task", HttpStatus.FORBIDDEN);
        }
        
        taskFileRepository.setFilePathByTaskId(taskId, newFilePath);
    }

    /**
     * Updates the file path for a specific file attached to a task.
     * Only project managers and users assigned to the task can update files.
     *
     * @param taskId The ID of the task
     * @param fileId The ID of the file to update
     * @param newFilePath The new file path to set
     * @param user The user performing the update
     * @throws ApiException if task or file not found, user lacks permission, or file doesn't belong to task
     */
    @Transactional
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

    /**
     * Checks if a file with the specified path exists in the system.
     *
     * @param filePath The file path to check
     * @return true if a file with this path exists, false otherwise
     */
    @Transactional
    public boolean doesFileExist(String filePath) {
        return taskFileRepository.existsByFilePath(filePath);
    }

    /**
     * Deletes a task file by its file path.
     * Only project managers and users assigned to the task can delete files.
     *
     * @param filePath The path of the file to delete
     * @param taskId The ID of the task the file belongs to
     * @param user The user attempting the deletion
     * @throws ApiException if file not found or user lacks permission
     */
    @Transactional
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

    /**
     * Deletes a task file by its ID.
     * Only project managers and users assigned to the task can delete files.
     *
     * @param taskId The ID of the task
     * @param fileId The ID of the file to delete
     * @param user The user attempting the deletion
     * @throws ApiException if task or file not found, user lacks permission, or file doesn't belong to task
     */
    @Transactional
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

    /**
     * Retrieves all files associated with a specific task.
     * Only project members and administrators can view task files.
     *
     * @param taskId The ID of the task
     * @param user The user requesting the files
     * @return List of TaskFile entities associated with the task
     * @throws ApiException if task not found or user lacks permission
     */
    @Transactional
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

    /**
     * Retrieves a specific file attached to a task by its ID.
     * Only project members and administrators can view task files.
     *
     * @param taskId The ID of the task
     * @param fileId The ID of the file to retrieve
     * @param user The user requesting the file
     * @return The requested TaskFile entity
     * @throws ApiException if task or file not found, or user lacks permission
     */
    @Transactional
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

    /**
     * Retrieves a file by its path.
     * Only administrators can access files directly by path.
     *
     * @param filePath The path of the file to retrieve
     * @param user The user requesting the file
     * @return The requested TaskFile entity
     * @throws ApiException if file not found, file path is null, or user lacks permission
     */
    @Transactional
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

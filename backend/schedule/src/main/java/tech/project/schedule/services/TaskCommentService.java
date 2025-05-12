package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.ProjectUserRole;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.task.Task;
import tech.project.schedule.model.task.TaskComment;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.TaskCommentRepository;
import tech.project.schedule.repositories.TaskRepository;
import tech.project.schedule.services.utils.GetProjectRole;
import tech.project.schedule.services.utils.PmAndAssigneeCheck;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service class for managing task comments within the scheduling system.
 * Provides functionality for adding, retrieving, and deleting comments on tasks,
 * with appropriate permission checks based on user roles and project membership.
 */
@Service
@RequiredArgsConstructor
public class TaskCommentService {
    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;

    /**
     * Adds a comment to a task.
     * Only project managers and users assigned to the task can add comments.
     *
     * @param taskId The ID of the task to comment on
     * @param user The user adding the comment
     * @param comment The comment entity to add
     * @return The saved comment entity
     * @throws ApiException if task not found or user lacks permission
     */
    @Transactional
    public TaskComment addComment(UUID taskId, User user, TaskComment comment){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId, user)){
            throw new ApiException("You are not allowed to add comments", HttpStatus.FORBIDDEN);
        }
        comment.setTask(task);
        comment.setUser(user);
        task.getComments().add(comment);

        comment = taskCommentRepository.save(comment);

        taskRepository.save(task);
        return comment;
    }
    
    /**
     * Deletes a specific comment from a task.
     * Only project managers and users assigned to the task can delete comments.
     *
     * @param taskId    The ID of the task containing the comment
     * @param user      The user performing the deletion
     * @param commentId The ID of the comment to delete
     * @throws ApiException if task or comment not found, or user lacks permission
     */
    @Transactional
    public void deleteComment(UUID taskId, User user, UUID commentId){
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found.", HttpStatus.NOT_FOUND));

        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException("Comment not found.", HttpStatus.NOT_FOUND));
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(taskId,user)){
            throw new ApiException("You are not allowed to delete this comment", HttpStatus.FORBIDDEN);
        }
        task.getComments().remove(comment);
        taskRepository.save(task);
    }

    /**
     * Deletes all comments for a specific task.
     * Only project managers can perform this operation.
     *
     * @param taskId The ID of the task whose comments should be deleted
     * @param user The user performing the deletion
     * @throws ApiException if task not found or user lacks permission
     */
    @Transactional
    public void deleteAllCommentsForTask(UUID taskId, User user) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found.", HttpStatus.NOT_FOUND));
        if(GetProjectRole.getProjectRole(user,task.getProject()) != ProjectUserRole.PM){
            throw new ApiException("You are not allowed to delete comments in this task", HttpStatus.FORBIDDEN);
        }
        taskCommentRepository.deleteAllByTask_Id(taskId);
    }

    /**
     * Retrieves all comments for a specific task.
     * Only administrators and project members can view task comments.
     *
     * @param taskId The ID of the task
     * @param user The user requesting the comments
     * @return List of comments for the specified task
     * @throws ApiException if task not found or user lacks permission
     */
    public List<TaskComment> getCommentsForTask(UUID taskId, User user) {
        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", HttpStatus.NOT_FOUND));
        boolean isInProject = task.getProject().getMembers().containsKey(user.getId());
        if(!isAdmin&&!isInProject){
            throw new ApiException("You are not allowed to view comments", HttpStatus.FORBIDDEN);
        }
        return taskCommentRepository.findAllByTask_Id(taskId);
    }

    /**
     * Retrieves comments made by a specific user.
     * Access is limited based on context - administrators can see all comments,
     * users can see their own, and project members can see comments within shared projects.
     *
     * @param currUser The user requesting the comments
     * @param otherUser The user whose comments are being requested
     * @return List of accessible comments for the specified user
     */
    public List<TaskComment> getUserComments(User currUser, User otherUser) {
        boolean isAdmin = currUser.getGlobalRole() == GlobalRole.ADMIN;
        if(isAdmin||currUser.getId().equals(otherUser.getId())){return taskCommentRepository.findAllByUser_Id(otherUser.getId());}

        List<TaskComment> userComments = new ArrayList<>();
        List<TaskComment> allComments = taskCommentRepository.findAllByUser_Id(otherUser.getId());

        for(TaskComment comment : allComments) {
            Project commentProject = comment.getTask().getProject();
            if(commentProject.getMembers().containsKey(currUser.getId())
                    && commentProject.getMembers().containsKey(otherUser.getId())){
                userComments.add(comment);
            }
        }
        
        return userComments;
    }

    /**
     * Retrieves a specific comment by its ID.
     * Only project managers and users assigned to the task can view comments.
     *
     * @param commentId The ID of the comment to retrieve
     * @param user The user requesting the comment
     * @return The requested comment entity
     * @throws ApiException if comment not found or user lacks permission
     */
    public TaskComment getCommentById(UUID commentId, User user) {
        TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException("Comment not found", HttpStatus.NOT_FOUND));
        if(PmAndAssigneeCheck.checkIfNotPmAndAssignee(comment.getTask().getId(), user)){
            throw new ApiException("You are not allowed to view this comment", HttpStatus.FORBIDDEN);
        }
        return comment;

    }

}


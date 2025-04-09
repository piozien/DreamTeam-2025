package tech.project.schedule.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.project.schedule.dto.task.TaskCommentDTO;
import tech.project.schedule.model.task.TaskComment;
import tech.project.schedule.services.TaskCommentService;

import java.util.UUID;

@RestController
@RequestMapping("/api/task-comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService taskCommentService;

    // Dodawanie komentarza do zadania
    @PostMapping("/{taskId}/user/{userId}")
    public ResponseEntity<TaskCommentDTO> addComment(
            @PathVariable UUID taskId,
            @PathVariable UUID userId,
            @RequestBody String content) {

        TaskComment comment = taskCommentService.addComment(taskId, userId, content);

        TaskCommentDTO commentDTO = new TaskCommentDTO(
                comment.getId(),
                comment.getTask().getId(),
                comment.getUser().getId(),
                comment.getComment(),
                comment.getCreatedAt()
        );

        return new ResponseEntity<>(commentDTO, HttpStatus.CREATED);
    }

    // Usuwanie pojedynczego komentarza po jego ID
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        taskCommentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    // Usuwanie wszystkich komentarzy dla danego zadania
    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Void> deleteAllCommentsForTask(@PathVariable UUID taskId) {
        taskCommentService.deleteAllCommentsForTask(taskId);
        return ResponseEntity.noContent().build();
    }
}
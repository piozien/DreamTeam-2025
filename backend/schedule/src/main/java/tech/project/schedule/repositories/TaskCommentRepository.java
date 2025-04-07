package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.task.TaskComment;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    List<TaskComment> findAllByTask_Id(UUID taskId);

    List<TaskComment> findAllByUser_Id(UUID userId);

    void deleteAllByTask_Id(UUID taskId);               //delete all comments attached to task
}
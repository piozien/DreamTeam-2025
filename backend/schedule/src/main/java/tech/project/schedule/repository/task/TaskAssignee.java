package tech.project.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.task.TaskAssignee;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, UUID> {
    List<TaskAssignee> findByTaskId(UUID taskId);

    List<TaskAssignee> findByUserId(UUID userId);

    boolean existsByTaskIdAndUserId(UUID taskId, UUID userId);
}

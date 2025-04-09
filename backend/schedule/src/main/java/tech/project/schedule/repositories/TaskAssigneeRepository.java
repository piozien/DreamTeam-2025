package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.task.TaskAssignee;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, UUID> {


    List<TaskAssignee> findAllByTask_Id(UUID taskId);           //All Users in single Task


    List<TaskAssignee> findAllByUser_Id(UUID userId);           //All Task in single User

    void deleteByTask_IdAndUser_Id(UUID taskId, UUID userId);   //

}

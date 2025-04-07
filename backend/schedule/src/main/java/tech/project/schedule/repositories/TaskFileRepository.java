package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.model.task.TaskFile;

import java.util.UUID;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, UUID> {
    TaskFile findByTask_Id(UUID taskId);

    TaskFile findByFilePath(String filePath);

    boolean existsByFilePath(String filePath);

    void deleteByFilePath(String filePath);

    void deleteByTask_Id(UUID taskId);


    @Modifying
    @Transactional
    @Query("UPDATE TaskFile tf SET tf.filePath = :filePath WHERE tf.task.id = :taskId")
    void setFilePath(UUID taskId, String filePath);
}
package tech.project.schedule.repositories;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.task.TaskFile;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, UUID> {

    TaskFile findByTaskId(UUID taskId);

    List<TaskFile> findAllByTaskId(UUID taskId);

    TaskFile findByFilePath(String filePath) ;

    boolean existsByFilePath(String filePath);

    void deleteByFilePath(String filePath);

    void deleteByTaskId(UUID taskId);


    @Modifying
    @Transactional
    @Query("UPDATE TaskFile tf SET tf.filePath = :filePath WHERE tf.task.id = :taskId")
    void setFilePathByTaskId(UUID taskId, String filePath);
    
    @Modifying
    @Transactional
    @Query("UPDATE TaskFile tf SET tf.filePath = :filePath WHERE tf.id = :id")
    void setFilePathById(UUID id, String filePath);
}
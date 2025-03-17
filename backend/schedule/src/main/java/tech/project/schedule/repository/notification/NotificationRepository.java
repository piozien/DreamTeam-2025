package tech.project.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.notification.Notification;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserId(UUID userId);

    List<Notification> findByUserIdAndIsRead(UUID userId, Boolean isRead);
}

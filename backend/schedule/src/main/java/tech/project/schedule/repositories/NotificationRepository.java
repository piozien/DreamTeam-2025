package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.user.User;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing Notification entities.
 * Provides database operations for storing and retrieving system notifications.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    /**
     * Finds all notifications for a specific user.
     * 
     * @param user The user whose notifications to retrieve
     * @return List of notifications for the specified user
     */
    List<Notification> findByUser(User user);
}

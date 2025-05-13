package tech.project.schedule.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.project.Project;
import tech.project.schedule.model.user.User;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for accessing and managing Notification entities.
 * Extends JpaRepository to leverage standard CRUD operations and pagination support
 * for notifications in the database. Provides methods for retrieving notifications
 * filtered by specific criteria.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUser(User user);
}

package tech.project.schedule.model.notification;

import jakarta.persistence.*;
import lombok.Data;
import tech.project.schedule.model.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing user notifications in the scheduling system.
 * Notifications are used to inform users about events, changes, or actions
 * that require their attention within projects or tasks.
 */
@Entity
@Table(name = "Notifications")
@Data
public class Notification {
     /**
     * Unique identifier for the notification.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

     /**
     * The user to whom this notification is directed.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The content of the notification message.
     */
    @Column(nullable = false)
    private String message;

     /**
     * Timestamp indicating when the notification was created.
     * Automatically populated during entity creation.
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

      
    /**
     * Flag indicating whether the user has read the notification.
     * Defaults to false for new notifications.
     */
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * Sets the creation timestamp when a notification is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

package tech.project.schedule.model.notification;

import jakarta.persistence.*;
import lombok.*;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity class representing a notification in the system.
 * Notifications are generated in response to various events (e.g., project updates,
 * task assignments) and are directed to specific users. The system uses these
 * records to display alerts and keep users informed about relevant activities.
 */
@Entity
@Table(name = "Notifications")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    /**
     * Lifecycle hook that executes before the entity is persisted.
     * Sets the creation timestamp to the current date and time.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

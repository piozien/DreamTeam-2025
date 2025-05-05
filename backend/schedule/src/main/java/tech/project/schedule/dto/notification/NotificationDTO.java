package tech.project.schedule.dto.notification;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object that represents a notification in the system.
 * Contains all essential information about a notification including its
 * unique identifier, message content, creation timestamp, and read status.
 */
public record NotificationDTO (
        UUID id,
        String message,
        LocalDateTime createdAt,
        Boolean isRead
){
}

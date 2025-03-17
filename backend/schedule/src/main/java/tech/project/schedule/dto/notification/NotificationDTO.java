package tech.project.schedule.dto.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDTO (
        UUID id,
        String message,
        LocalDateTime createdAt,
        Boolean isRead
){
}

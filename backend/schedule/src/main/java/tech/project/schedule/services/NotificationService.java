package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.user.User;

import java.util.UUID;
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;


    public void sendNotification(UUID userId, Notification notification) {
        log.info("Sending notification to {} with payload {}", userId, notification);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/notification",
                notification
        );
    }

    public void sendNotificationToUser(User user, NotificationStatus status, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .status(status)
                .message(message)
                .build();
        sendNotification(user.getId(), notification);
    }
}

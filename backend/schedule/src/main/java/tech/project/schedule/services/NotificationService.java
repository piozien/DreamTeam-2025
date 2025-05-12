package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.NotificationRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void sendNotification(UUID userId, Notification notification) {
            log.info("Sending notification to {} with payload {}", userId, notification);
            Map<String, Object> payload = new HashMap<>();
            payload.put("id", notification.getId());
            payload.put("status", notification.getStatus());
            payload.put("message", notification.getMessage());
            payload.put("userId", notification.getUser().getId().toString());
            payload.put("createdAt", notification.getCreatedAt().toString());
            payload.put("isRead", notification.getIsRead());

            String destination = String.format("/user/%s/queue/notifications", userId);
            messagingTemplate.convertAndSend(destination, payload);
            
        }
    

    @Transactional
    public Notification sendNotificationToUser(User user, NotificationStatus status, String message) {
        
            Notification notification = Notification.builder()
                    .user(user)
                    .status(status)
                    .message(message)
                    .isRead(false)
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            sendNotification(user.getId(), savedNotification);
            return savedNotification;
            
    }
}

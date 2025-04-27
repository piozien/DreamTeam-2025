package tech.project.schedule.services;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.repositories.TaskAssigneeRepository;

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
}

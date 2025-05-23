package tech.project.schedule.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.NotificationRepository;
import tech.project.schedule.repositories.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void sendNotification(UUID userId, Notification notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notification.getId());
        payload.put("status", notification.getStatus());
        payload.put("message", notification.getMessage());
        payload.put("userId", notification.getUser().getId());
        payload.put("createdAt", notification.getCreatedAt().toString());
        payload.put("isRead", notification.getIsRead());
        String destination = String.format("/user/%s/queue/notifications", userId);
        messagingTemplate.convertAndSend(destination, payload);
    }

    @Transactional
    public void sendNotificationToUser(UUID userId, NotificationStatus status, String message) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ApiException("User not found", HttpStatus.NOT_FOUND)
        );
        Notification notification = Notification.builder()
                .user(user)
                .status(status)
                .message(message)
                .isRead(false)
                .build();
        Notification savedNotification = notificationRepository.save(notification);
        sendNotification(user.getId(), savedNotification);
    }

    @Transactional
    public List<Notification> getUserNotifications(UUID currUserId, User authenticatedUser) {
        boolean isAdmin = authenticatedUser.getGlobalRole() == GlobalRole.ADMIN;

        if(!isAdmin && !authenticatedUser.getId().equals(currUserId)) {
            throw new ApiException("You do not have permission to view notifications", HttpStatus.UNAUTHORIZED);
        }
        User currUser = userRepository.findById(currUserId).orElseThrow(
                () -> new ApiException("User not found", HttpStatus.NOT_FOUND)
        );
        return notificationRepository.findByUser(currUser);
    }

    @Transactional
    public Notification markNotificationAsRead(User user, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(
                () -> new ApiException("Notification not found", HttpStatus.NOT_FOUND)
        );
        if(!user.getId().equals(notification.getUser().getId())) {
            throw new ApiException("You do not have permission to view notifications", HttpStatus.UNAUTHORIZED);
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
        return notification;
    }

    @Transactional
    public List<Notification> markAllNotificationsAsRead(User user) {
        List<Notification> notifications = notificationRepository.findByUser(user);
        if(notifications.isEmpty()) {
            throw new ApiException("No notifications found", HttpStatus.NOT_FOUND);
        }
        for(Notification notification : notifications) {
            notification.setIsRead(true);
        }
        return notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void deleteNotification(User user, UUID notificationId){
        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
                Notification notification = notificationRepository.findById(notificationId).orElseThrow(
                () -> new ApiException("Notification not found", HttpStatus.NOT_FOUND)
        );
        if(!user.getId().equals(notification.getUser().getId()) && !isAdmin) {
            throw new ApiException("You do not have permission to view these notifications", HttpStatus.UNAUTHORIZED);
        }
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAllNotifications(User user) {
        boolean isAdmin = user.getGlobalRole() == GlobalRole.ADMIN;
        List<Notification> notifications = notificationRepository.findByUser(user);
        if(notifications.isEmpty()) {
            throw new ApiException("No notifications found", HttpStatus.NOT_FOUND);
        }
        if(!isAdmin&&notifications.stream()
                .anyMatch(notification -> notification.getUser().getId().equals(user.getId()))) {
            throw new ApiException("You do not have permission to view notifications", HttpStatus.UNAUTHORIZED);
        }
        notificationRepository.deleteAll(notifications);
    }
}

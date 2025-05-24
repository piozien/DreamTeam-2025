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

/**
 * Service for managing user notifications throughout the application.
 * Handles both persistence of notifications in the database and real-time
 * delivery to users via WebSockets. Provides methods for creating, reading,
 * updating, and deleting notifications with appropriate permission checks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

      /**
     * Sends a notification to a user via WebSocket.
     * Converts notification entity to a simplified payload format and
     * delivers it to the user's specific notification queue.
     * 
     * @param userId ID of the user to receive the notification
     * @param notification The notification entity to send
     */
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

     /**
     * Creates and sends a notification to a specific user.
     * Persists the notification in the database and delivers it
     * via WebSocket if the user is currently connected.
     * 
     * @param userId ID of the user to receive the notification
     * @param status The type of notification being sent
     * @param message The content of the notification
     */
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

    /**
     * Retrieves all notifications for a specific user.
     * Only the user themselves or an admin can access a user's notifications.
     * 
     * @param currUserId ID of the user whose notifications to retrieve
     * @param authenticatedUser The user making the request
     * @return List of notifications for the specified user
     */
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

     /**
     * Marks a specific notification as read.
     * Only the notification recipient can mark it as read.
     * 
     * @param user The user performing the action
     * @param notificationId ID of the notification to mark as read
     * @return The updated notification entity
     */
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

    /**
     * Marks all of a user's notifications as read.
     * 
     * @param user The user whose notifications to mark as read
     * @return List of updated notification entities
     */
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

     /**
     * Deletes a specific notification.
     * Only the notification recipient or an admin can delete a notification.
     * 
     * @param user The user performing the deletion
     * @param notificationId ID of the notification to delete
     */
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

    /**
     * Deletes all notifications for a user.
     * Only the user themselves or an admin can delete a user's notifications.
     * 
     * @param user The user whose notifications to delete
     */
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

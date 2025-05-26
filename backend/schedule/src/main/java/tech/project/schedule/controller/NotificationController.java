package tech.project.schedule.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.project.schedule.dto.notification.NotificationDTO;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.NotificationService;
import tech.project.schedule.dto.mappers.NotificationMapper;
import tech.project.schedule.utils.UserUtils;

import java.util.List;
import java.util.UUID;

/**
 * REST controller managing user notifications.
 * Provides endpoints for retrieving, updating, and deleting notifications
 * with appropriate authorization checks for user-specific operations.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Retrieves notifications for a specific user.
     * Validates that the requesting user is authorized to view these notifications.
     * 
     * @param userId ID of the user whose notifications to retrieve
     * @param authenticatedUserId ID of the user making the request
     * @return List of notifications as DTOs
     */
    @GetMapping("")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(
            @RequestParam UUID userId,
            @RequestParam UUID authenticatedUserId) {
        User authenticatedUser = userRepository.findById(authenticatedUserId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        UserUtils.assertAuthorized(authenticatedUser);
        List<Notification> notifications = notificationService.getUserNotifications(userId, authenticatedUser);
        return ResponseEntity.ok(NotificationMapper.notificationToDtoList(notifications));
    }

     /**
     * Marks a specific notification as read.
     * 
     * @param notificationId ID of the notification to mark as read
     * @param userId ID of the user making the request
     * @return The updated notification as DTO
     */
    @PutMapping("/{notificationId}")
    public ResponseEntity<NotificationDTO> markNotificationAsRead(
            @PathVariable UUID notificationId,
            @RequestParam UUID userId) {
        User authenticatedUser = userRepository.findById(userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        UserUtils.assertAuthorized(authenticatedUser);
        Notification notification = notificationService.markNotificationAsRead(authenticatedUser, notificationId);
        return ResponseEntity.ok(NotificationMapper.notificationToDto(notification));
    }

    /**
     * Marks all notifications for a user as read.
     * 
     * @param userId ID of the user making the request
     * @return List of updated notifications as DTOs
     */
    @PutMapping("/read-all")
    public ResponseEntity<List<NotificationDTO>> markAllNotificationsAsRead(
            @RequestParam UUID userId) {
        User authenticatedUser = userRepository.findById(userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        UserUtils.assertAuthorized(authenticatedUser);
        List<Notification> notifications = notificationService.markAllNotificationsAsRead(authenticatedUser);
        return ResponseEntity.ok(NotificationMapper.notificationToDtoList(notifications));
    }

    /**
     * Deletes a specific notification.
     * 
     * @param notificationId ID of the notification to delete
     * @param userId ID of the user making the request
     * @return Empty response with 204 No Content status
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String> deleteNotification(
            @PathVariable UUID notificationId,
            @RequestParam UUID userId) {
        User authenticatedUser = userRepository.findById(userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        UserUtils.assertAuthorized(authenticatedUser);
        notificationService.deleteNotification(authenticatedUser, notificationId);
        return ResponseEntity.noContent().build();
    }

     /**
     * Deletes all notifications for a user.
     * 
     * @param userId ID of the user making the request
     * @return Empty response with 204 No Content status
     */
    @DeleteMapping("/all")
    public ResponseEntity<String> deleteAllNotifications(
            @RequestParam UUID userId) {
        User authenticatedUser = userRepository.findById(userId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        UserUtils.assertAuthorized(authenticatedUser);
        notificationService.deleteAllNotifications(authenticatedUser);
        return ResponseEntity.noContent().build();
    }
} 

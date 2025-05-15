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


@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

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
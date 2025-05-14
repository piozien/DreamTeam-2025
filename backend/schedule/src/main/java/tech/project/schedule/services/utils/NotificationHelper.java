package tech.project.schedule.services.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.project.schedule.model.enums.NotificationStatus;
import tech.project.schedule.model.notification.Notification;
import tech.project.schedule.model.user.User;
import tech.project.schedule.services.NotificationService;

/**
 * Helper class for creating and sending notifications across the application.
 * Provides standardized notification messages for various actions and events.
 */
@Component
@RequiredArgsConstructor
public class NotificationHelper {
    private final NotificationService notificationService;

    /**
     * Sends a generic notification to a user.
     *
     * @param recipient The user to receive the notification
     * @param status The type of notification
     * @param message The notification message content
     * @return The created notification entity
     */
    public Notification notifyUser(User recipient, NotificationStatus status, String message) {
        return notificationService.sendNotificationToUser(recipient.getId(), status, message);
    }

    /**
     * Sends a notification about a task to its assignee with predefined message formats.
     *
     * @param assignee The task assignee to notify
     * @param status The type of notification 
     * @param taskName The name of the task
     * @return The created notification
     */
    public Notification notifyTaskAssignee(User assignee, NotificationStatus status, String taskName) {
        String message = switch (status) {
            case TASK_ASSIGNEE_ADDED -> "Zostałeś dodany do zadania " + taskName;
            case TASK_UPDATED -> "Zadanie " + taskName + " zostało zaktualizowane";
            case TASK_COMPLETED -> "Zadanie " + taskName + " zostało oznaczone jako zakończone";
            case TASK_DELETED -> "Zadanie " + taskName + " zostało usunięte";
            case TASK_COMMENT_ADDED -> "Dodano nowy komentarz do zadania " + taskName;
            case TASK_COMMENT_UPDATED -> "Zaktualizowano komentarz w zadaniu " + taskName;
            case TASK_COMMENT_DELETED -> "Usunięto komentarz z zadania " + taskName;
            case TASK_FILE_UPLOADED -> "Dodano nowy plik do zadania " + taskName;
            case TASK_FILE_UPDATED -> "Zaktualizowano plik w zadaniu " + taskName;
            case TASK_FILE_DELETED -> "Usunięto plik z zadania " + taskName;
            case TASK_DEPENDENCY_ADDED -> "Dodano nową zależność do zadania " + taskName;
            case TASK_DEPENDENCY_DELETED -> "Usunięto zależność z zadania " + taskName;
            case TASK_DEPENDENCY_UPDATED -> "Zaktualizowano zależność w zadaniu " + taskName;
            default -> "Nastąpiła zmiana w zadaniu " + taskName;
        };

        return notifyUser(assignee, status, message);
    }

    /**
     * Notifies a project member about project changes with predefined message formats.
     *
     * @param member The project member to notify
     * @param status The type of notification
     * @param projectName The name of the project
     * @return The created notification
     */
    public Notification notifyProjectMember(User member, NotificationStatus status, String projectName) {
        String message = switch (status) {
            case PROJECT_MEMBER_ADDED -> "Zostałeś dodany do projektu " + projectName;
            case PROJECT_UPDATED -> "Projekt " + projectName + " został zaktualizowany";
            case PROJECT_DELETED -> "Projekt " + projectName + " został usunięty";
            case PROJECT_CREATED -> "Stworzono projekt: " + projectName + " pomyślnie";
            default -> "Nastąpiła zmiana w projekcie " + projectName;
        };

        return notifyUser(member, status, message);
    }
    
    /**
     * Sends an error notification to a user.
     *
     * @param user The user to notify about the error
     * @param errorType Short description of the error type
     * @param details Detailed information about what went wrong
     * @return The created notification
     */
    public Notification notifyError(User user, String errorType, String details) {
        String message = "Błąd: " + errorType + ". " + details;
        return notifyUser(user, NotificationStatus.ERROR, message);
    }
    
    /**
     * Notifies a user about permission issues.
     *
     * @param user The user to notify
     * @param resource The resource they tried to access
     * @param action The action they tried to perform
     * @return The created notification
     */
    public Notification notifyPermissionDenied(User user, String resource, String action) {
        String message = "Brak uprawnień: nie możesz " + action + " dla " + resource;
        return notifyUser(user, NotificationStatus.PERMISSION_DENIED, message);
    }
    
    /**
     * Sends a system notification to a user.
     *
     * @param user The user to notify
     * @param title The notification title
     * @param content The notification content
     * @return The created notification
     */
    public Notification notifySystem(User user, String title, String content) {
        String message = title + ": " + content;
        return notifyUser(user, NotificationStatus.SYSTEM, message);
    }
} 
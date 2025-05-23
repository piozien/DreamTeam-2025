package tech.project.schedule.services.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.project.schedule.model.enums.NotificationStatus;
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
     * @param status    The type of notification
     * @param message   The notification message content
     */
    public void notifyUser(User recipient, NotificationStatus status, String message) {
        notificationService.sendNotificationToUser(recipient.getId(), status, message);
    }

    /**
     * Sends a notification about a task to its assignee with predefined message formats.
     *
     * @param assignee The task assignee to notify
     * @param status   The type of notification
     * @param taskName The name of the task
     */
    public void notifyTaskAssignee(User assignee, NotificationStatus status, String taskName) {
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

        notifyUser(assignee, status, message);
    }

    /**
     * Notifies a project member about project changes with predefined message formats.
     *
     * @param member      The project member to notify
     * @param status      The type of notification
     * @param projectName The name of the project
     */
    public void notifyProjectMember(User member, NotificationStatus status, String projectName) {
        String message = switch (status) {
            case PROJECT_MEMBER_ADDED -> "Zostałeś dodany do projektu " + projectName;
            case PROJECT_UPDATED -> "Projekt " + projectName + " został zaktualizowany";
            case PROJECT_DELETED -> "Projekt " + projectName + " został usunięty";
            case PROJECT_CREATED -> "Stworzono projekt: " + projectName + " pomyślnie";
            default -> "Nastąpiła zmiana w projekcie " + projectName;
        };

        notifyUser(member, status, message);
    }
}

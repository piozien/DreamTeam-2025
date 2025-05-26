package tech.project.schedule.dto.mappers;

import tech.project.schedule.dto.notification.NotificationDTO;
import tech.project.schedule.model.notification.Notification;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between Notification domain entities and DTOs.
 * Provides static conversion methods to transform notification data between
 * the internal domain model and the external API representation.
 * 
 * Using these mappers keeps entity-to-DTO conversion logic centralized and
 * consistent throughout the application, separating persistence concerns
 * from API response formatting.
 */
public class NotificationMapper {
     /**
     * Converts a single Notification entity to a NotificationDTO.
     * Extracts only the fields necessary for client display, omitting
     * sensitive or unnecessary information.
     * 
     * @param notification The domain entity to convert
     * @return A DTO representation of the notification
     */
    public static NotificationDTO notificationToDto(Notification notification){
        return new NotificationDTO(
          notification.getId(),
          notification.getMessage(),
          notification.getCreatedAt(),
          notification.getIsRead()
        );
    }

    /**
     * Converts a list of Notification entities to NotificationDTO objects.
     * Uses Java streams for efficient batch conversion.
     * 
     * @param notifications The list of domain entities to convert
     * @return A list of corresponding DTOs
     */
    public static List<NotificationDTO> notificationToDtoList(List<Notification> notifications) {
        return notifications.stream()
                .map(NotificationMapper::notificationToDto)
                .collect(Collectors.toList());
    }
}

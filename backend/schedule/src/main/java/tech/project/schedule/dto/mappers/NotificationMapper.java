package tech.project.schedule.dto.mappers;

import tech.project.schedule.dto.notification.NotificationDTO;
import tech.project.schedule.model.notification.Notification;

import java.util.List;
import java.util.stream.Collectors;

public class NotificationMapper {
    public static NotificationDTO notificationToDto(Notification notification){
        return new NotificationDTO(
          notification.getId(),
          notification.getMessage(),
          notification.getCreatedAt(),
          notification.getIsRead()
        );
    }
    
    public static List<NotificationDTO> notificationToDtoList(List<Notification> notifications) {
        return notifications.stream()
                .map(NotificationMapper::notificationToDto)
                .collect(Collectors.toList());
    }
}

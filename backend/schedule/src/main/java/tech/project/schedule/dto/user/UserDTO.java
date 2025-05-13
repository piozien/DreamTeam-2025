package tech.project.schedule.dto.user;

import tech.project.schedule.model.enums.GlobalRole;

import java.util.UUID;

import tech.project.schedule.model.enums.UserStatus;

/**
 * Data Transfer Object for user information.
 * Used to transfer user data between the service layer and API controllers,
 * particularly in API responses where sensitive information like passwords 
 * should be excluded. Contains essential user attributes for display and identification.
 */
public record UserDTO(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        GlobalRole globalRole,
        UserStatus userStatus
) {
}

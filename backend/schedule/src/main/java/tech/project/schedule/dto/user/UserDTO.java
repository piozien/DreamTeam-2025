package tech.project.schedule.dto.user;

import tech.project.schedule.model.enums.GlobalRole;

import java.util.UUID;

import tech.project.schedule.model.enums.UserStatus;

/**
 * Data Transfer Object that represents user information for API responses.
 * Contains essential user attributes without sensitive information like passwords.
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

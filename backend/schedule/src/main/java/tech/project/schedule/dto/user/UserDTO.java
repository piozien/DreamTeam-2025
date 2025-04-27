package tech.project.schedule.dto.user;

import tech.project.schedule.model.enums.GlobalRole;

import java.util.UUID;

/**
 * Data Transfer Object that represents a user in the system.
 * Contains essential user information including identification details,
 * personal information, credentials, and system-wide role.
 */
public record UserDTO(
        UUID id,
        String firstName,
        String lastName,
        String username,
        String email,
        GlobalRole globalRole
) {
}

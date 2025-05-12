package tech.project.schedule.dto.user;

import tech.project.schedule.model.enums.GlobalRole;

import java.util.UUID;

import tech.project.schedule.model.enums.UserStatus;

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

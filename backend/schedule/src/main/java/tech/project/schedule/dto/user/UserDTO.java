package tech.project.schedule.dto.user;

import tech.project.schedule.model.enums.GlobalRole;

import java.util.UUID;

public record UserDTO(
        UUID id,
        String name,
        String email,
        GlobalRole globalRole
) {}
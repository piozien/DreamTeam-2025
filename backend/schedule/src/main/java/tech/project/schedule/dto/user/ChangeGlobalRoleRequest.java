package tech.project.schedule.dto.user;

import java.util.UUID;

/**
 * DTO for changing a user's global role.
 */
public class ChangeGlobalRoleRequest {
    public UUID userId;
    public String newRole;
}

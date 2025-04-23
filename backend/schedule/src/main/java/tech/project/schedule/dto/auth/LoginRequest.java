package tech.project.schedule.dto.auth;

import lombok.Data;

/**
 * Data Transfer Object used for user login requests.
 * Contains the credentials (email and password) needed to authenticate a user.
 */
@Data
public class LoginRequest {
    private String email;
    private String password;
}

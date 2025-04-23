package tech.project.schedule.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * Data Transfer Object that contains user information needed for registration.
 * This class captures all required fields for creating a new user account in the system.
 */
@Getter
@AllArgsConstructor
public class RegistrationRequest {
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String password;
    private final String email;
}

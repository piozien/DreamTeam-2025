package tech.project.schedule.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegistrationRequest {
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String password;
    private final String email;
}

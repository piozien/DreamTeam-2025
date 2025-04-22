package tech.project.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.project.schedule.dto.auth.HealthResponseDTO;
import tech.project.schedule.dto.auth.LoginRequest;
import tech.project.schedule.dto.auth.LoginResponseDTO;
import tech.project.schedule.dto.auth.RegistrationRequest;
import tech.project.schedule.dto.auth.RegistrationResponseDTO;
import tech.project.schedule.dto.user.UserDTO;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller responsible for managing authentication processes and user operations.
 * Provides endpoints for user registration, login, system health checking,
 * and retrieving the list of all users.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

      /**
     * Registers a new user in the system.
     * 
     * @param request Object containing user registration data
     * @return ResponseEntity containing the registration result message
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDTO> register(@Valid @RequestBody RegistrationRequest request) {
        String result = userService.register(request);
        return ResponseEntity.ok(new RegistrationResponseDTO(result));
    }
    

    /**
     * Authenticates a user and creates a session.
     * 
     * @param request Object containing login credentials (email/username and password)
     * @return ResponseEntity containing information about the authenticated user
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request);
        return ResponseEntity.ok(new LoginResponseDTO(user.getId(), user.getEmail(), user.getName(), user.getUsername()));
    }
    
    /**
     * Provides information about the application's operational status.
     * Used for monitoring and health checking of the system.
     * 
     * @return ResponseEntity containing current system status and timestamp
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponseDTO> healthCheck() {
        return ResponseEntity.ok(new HealthResponseDTO(
                "UP",
                LocalDateTime.now()
        ));
    }

    /**
     * Retrieves a list of all users in the system.
     * 
     * @param userId Identifier of the user making the request
     * @return ResponseEntity containing a list of all users as DTOs
     * @throws ApiException when the requesting user is not found
     */
    @GetMapping("/all-users")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam UUID userId
    ) {
        // ToDo: User will have account status GlobalRole.Admin can display inactive users
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        // Retrieve all users from the repository
        List<User> users = userRepository.findAll();
        
        // Map User entities to UserDTO objects to expose only necessary information
        List<UserDTO> userDTOs = users.stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getGlobalRole()
                ))
                .toList();

        return ResponseEntity.ok(userDTOs);
    }
}

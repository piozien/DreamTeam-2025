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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDTO> register(@Valid @RequestBody RegistrationRequest request) {
        String result = userService.register(request);
        return ResponseEntity.ok(new RegistrationResponseDTO(result));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request);
        return ResponseEntity.ok(new LoginResponseDTO(user.getId(), user.getEmail(), user.getName(), user.getUsername()));
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponseDTO> healthCheck() {
        return ResponseEntity.ok(new HealthResponseDTO(
                "UP",
                LocalDateTime.now()
        ));
    }

    @GetMapping("/all-users")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam UUID userId
    ) {
        // ToDo: User will have account status GlobalRole.Admin can display inactive users
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        List<User> users = userRepository.findAll();

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

package tech.project.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import tech.project.schedule.dto.auth.HealthResponseDTO;
import tech.project.schedule.dto.auth.LoginRequest;
import tech.project.schedule.dto.auth.LoginResponseDTO;
import tech.project.schedule.dto.auth.RegistrationRequest;
import tech.project.schedule.dto.auth.RegistrationResponseDTO;
import tech.project.schedule.dto.auth.SetPasswordRequest;
import tech.project.schedule.dto.user.UserDTO;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.UserService;
import tech.project.schedule.security.JwtUtil;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import tech.project.schedule.utils.UserUtils;

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
    private final JwtUtil jwtUtil;


    @GetMapping("/oauth2-success")
    public ResponseEntity<?> oauth2Success(
            @AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        String email = principal.getEmail();
        String firstName = principal.getGivenName();
        String lastName = principal.getFamilyName();
        String username = email.substring(0, email.indexOf("@"));

        Optional<tech.project.schedule.model.user.User> userOpt = userRepository.findByEmail(email);
        tech.project.schedule.model.user.User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            user = new tech.project.schedule.model.user.User(
                    firstName != null ? firstName : username,
                    lastName != null ? lastName : "",
                    email,
                    "",
                    username
            );
            user.setUserStatus(tech.project.schedule.model.enums.UserStatus.AUTHORIZED);
            userRepository.save(user);
        }
        UserDTO userDto = new tech.project.schedule.dto.user.UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                user.getGlobalRole(),
                user.getUserStatus()
        );
        return ResponseEntity.ok(userDto);
    }


      /**
     * Registers a new user in the system.
     * 
     * @param request Object containing user registration data
     * @return ResponseEntity containing the registration result message
     */
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDTO> register(@RequestParam(required = false) UUID adminId,
                                                            @Valid @RequestBody RegistrationRequest request) {
        // TEMPORARY: Allow registration of the first user as ADMIN if there are no users in the system
        if (userRepository.count() == 0) {
    RegistrationRequest adminRequest = new RegistrationRequest(
        request.username(),
        request.firstName(),
        request.lastName(),
        request.email(),
        tech.project.schedule.model.enums.GlobalRole.ADMIN
    );
    String result = userService.register(adminRequest);
    return ResponseEntity.ok(new RegistrationResponseDTO("First admin registered! (temporary logic, remove after init)"));
}
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException("Admin user not found", HttpStatus.NOT_FOUND));
        if (admin.getGlobalRole() != tech.project.schedule.model.enums.GlobalRole.ADMIN) {
            throw new ApiException("Only ADMIN can register new users", HttpStatus.FORBIDDEN);
        }
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
        String token = jwtUtil.generateToken(user.getEmail(), Map.of(
                "userId", user.getId().toString(),
                "role", user.getGlobalRole().name(),
                "status", user.getUserStatus().name()
        ));
        return ResponseEntity.ok(new LoginResponseDTO(token, user.getId(), user.getEmail(), user.getFirstName() + " " + user.getLastName(), user.getUsername()));
    }

    /*
     * Allows a user to set a new password after registration using a link sent by email.
     *
     * @param request SetPasswordRequest containing email and new password
     * @return ResponseEntity with success message
     */
    /*
     * Endpoint to request password reset link (email).
     * Only AUTHORIZED or UNAUTHORIZED users can request password reset.
     * BLOCKED users receive 403.
     */
    /**
     * Unifies password reset and privacy: always return OK, only send mail for eligible users.
     */
    @PostMapping("/request-password-reset")
public ResponseEntity<String> requestPasswordReset(@RequestBody SetPasswordRequest request) {
    // Always return OK for privacy (do not leak user existence/status)
    userRepository.findByEmail(request.email()).ifPresent(user -> {
        if (user.getUserStatus() != tech.project.schedule.model.enums.UserStatus.BLOCKED) {
            userService.sendPasswordResetEmail(user);
        }
    });
    return ResponseEntity.ok("If the email exists and is eligible, a password reset link has been sent.");
}

    /*
     * Endpoint to set new password (from link).
     * BLOCKED users cannot set new password.
     */
    /**
     * Sets a new password for a user, used for both registration and forgotten password.
     * Only AUTHORIZED or UNAUTHORIZED users can set password. BLOCKED users cannot.
     */
    @PostMapping("/set-password")
    public ResponseEntity<String> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        if (user.getUserStatus() == tech.project.schedule.model.enums.UserStatus.BLOCKED) {
            throw new ApiException("Blocked users cannot change password", HttpStatus.FORBIDDEN);
        }
        userService.setPassword(request.email(), request.newPassword());
        return ResponseEntity.ok("Password has been set successfully.");
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
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        UserUtils.assertAuthorized(currentUser);
        if (currentUser.getUserStatus() == tech.project.schedule.model.enums.UserStatus.BLOCKED) {
            throw new ApiException("User is blocked and cannot perform this action", HttpStatus.FORBIDDEN);
        }

        List<User> users = userRepository.findAll();
        if (currentUser.getGlobalRole() != tech.project.schedule.model.enums.GlobalRole.ADMIN) {
            users = users.stream()
                    .filter(user -> user.getUserStatus() == tech.project.schedule.model.enums.UserStatus.AUTHORIZED)
                    .toList();
        }

        List<UserDTO> userDTOs = users.stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getGlobalRole(),
                        currentUser.getGlobalRole() == tech.project.schedule.model.enums.GlobalRole.ADMIN ? user.getUserStatus() : null
                ))
                .toList();

        return ResponseEntity.ok(userDTOs);
    }

    /**
     * Blocks (soft-deletes) a user by email. Only ADMIN can perform this action.
     *
     * @param adminId UUID of the admin performing the action
     * @param email   Email of the user to block
     * @return ResponseEntity with result message
     */
    @PostMapping("/block-user")
    public ResponseEntity<String> blockUser(
            @RequestParam UUID adminId,
            @RequestParam String email) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException("Admin user not found", HttpStatus.NOT_FOUND));
        if (admin.getGlobalRole() != tech.project.schedule.model.enums.GlobalRole.ADMIN) {
            throw new ApiException("Only ADMIN can block users", HttpStatus.FORBIDDEN);
        }
        userService.blockUser(email);
        return ResponseEntity.ok("User blocked successfully.");
    }
}

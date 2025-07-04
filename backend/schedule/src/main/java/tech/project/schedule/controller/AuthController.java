package tech.project.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;
import tech.project.schedule.dto.auth.HealthResponseDTO;
import tech.project.schedule.dto.auth.LoginRequest;
import tech.project.schedule.dto.auth.LoginResponseDTO;
import tech.project.schedule.dto.auth.RegistrationRequest;
import tech.project.schedule.dto.auth.RegistrationResponseDTO;
import tech.project.schedule.dto.auth.SetPasswordRequest;
import tech.project.schedule.dto.user.ChangeGlobalRoleRequest;
import tech.project.schedule.dto.user.UserDTO;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.GlobalRole;
import tech.project.schedule.model.enums.UserStatus;
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

import org.springframework.web.servlet.view.RedirectView;

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
    public RedirectView oauth2Success(@AuthenticationPrincipal OidcUser principal,
                                      @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        if (principal == null) {
            // Redirect to login page with an error
            return new RedirectView("http://localhost:4200/auth/login?error=oauth_failed");
        }
        String email = principal.getEmail();
        String firstName = principal.getGivenName();
        String lastName = principal.getFamilyName();
        // Try to get full name if firstName or lastName are missing
        Object nameObj = principal.getAttribute("name");
        if ((firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) && nameObj != null) {
            String nameAttr = nameObj.toString();
            if (!nameAttr.isBlank()) {
                String[] split = nameAttr.trim().split(" ", 2);
                if (firstName == null || firstName.isBlank()) firstName = split[0];
                if ((lastName == null || lastName.isBlank()) && split.length > 1) lastName = split[1];
                if ((lastName == null || lastName.isBlank()) && split.length == 1) lastName = "";
            }
        }
        // Use nickname if present, otherwise email as username
        String username = principal.getPreferredUsername();
        if (username == null || username.isBlank()) {
            username = email;
        }

        // Find or create user logic (keep as is)
        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            // Optionally update user details if they changed in Google
            user.setFirstName(firstName != null ? firstName : user.getFirstName());
            user.setLastName(lastName != null ? lastName : user.getLastName());
            // Ensure status is AUTHORIZED if they login successfully
            if (user.getUserStatus() == UserStatus.UNAUTHORIZED) {
                user.setUserStatus(UserStatus.AUTHORIZED);
            }
            // Save refresh token if available
            if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
                user.setGoogleRefreshToken(authorizedClient.getRefreshToken().getTokenValue());
            }
            userRepository.save(user);
        } else {
            user = new tech.project.schedule.model.user.User(
                    firstName != null ? firstName : "",
                    lastName != null ? lastName : "",
                    email,
                    "", // No password needed for OAuth users initially
                    username
            );
            user.setUserStatus(UserStatus.AUTHORIZED); // New users via OAuth are authorized
            user.setGlobalRole(GlobalRole.CLIENT); // Default role
            // Save refresh token if available
            if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
                user.setGoogleRefreshToken(authorizedClient.getRefreshToken().getTokenValue());
            }
            userRepository.save(user);
        }

        // Generate token (keep as is)
        String token = jwtUtil.generateToken(user.getEmail(), Map.of(
                "userId", user.getId().toString(),
                "role", user.getGlobalRole().name(),
                "status", user.getUserStatus().name(),
                // Optionally include name directly in token if useful for frontend
                "name", user.getFirstName() + " " + user.getLastName(),
                "preferred_username", user.getUsername()
        ));

        // Redirect back to frontend with the token
        String redirectUrl = "http://localhost:4200/auth/oauth-callback?token=" + token;
        return new RedirectView(redirectUrl);
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
        GlobalRole.ADMIN
    );
    String result = userService.register(adminRequest);
    return ResponseEntity.ok(new RegistrationResponseDTO("First admin registered! (temporary logic, remove after init)"));
}
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException("Admin user not found", HttpStatus.NOT_FOUND));
        if (admin.getGlobalRole() != GlobalRole.ADMIN) {
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
        return ResponseEntity.ok(new LoginResponseDTO(token, user.getId(), user.getEmail(),
                user.getFirstName() + " " + user.getLastName(), user.getUsername()));
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
        if (user.getUserStatus() != UserStatus.BLOCKED) {
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
        if (user.getUserStatus() == UserStatus.BLOCKED) {
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
        if (currentUser.getUserStatus() == UserStatus.BLOCKED) {
            throw new ApiException("User is blocked and cannot perform this action", HttpStatus.FORBIDDEN);
        }

        List<User> users = userRepository.findAll();
        if (currentUser.getGlobalRole() != GlobalRole.ADMIN) {
            users = users.stream()
                    .filter(user -> user.getUserStatus() == UserStatus.AUTHORIZED)
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
                        currentUser.getGlobalRole() == GlobalRole.ADMIN ? user.getUserStatus() : null
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
        if (admin.getGlobalRole() != GlobalRole.ADMIN) {
            throw new ApiException("Only ADMIN can block users", HttpStatus.FORBIDDEN);
        }
        userService.blockUser(email);
        return ResponseEntity.ok("User blocked successfully.");
    }
    /**
     * Changes the global role of a user. Only ADMIN can perform this action.
     */
    @PostMapping("/change-global-role")
    public ResponseEntity<String> changeGlobalRole(@RequestParam UUID adminId,
                                                   @RequestBody ChangeGlobalRoleRequest request) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException("Admin user not found", HttpStatus.NOT_FOUND));
        if (admin.getGlobalRole() != GlobalRole.ADMIN) {
            throw new ApiException("Only ADMIN can change global roles", HttpStatus.FORBIDDEN);
        }
        userService.changeGlobalRole(request);
        return ResponseEntity.ok("Global role changed successfully");
    }

    /**
     * Authorizes a user (sets status to AUTHORIZED). Only ADMIN can perform this action.
     */
    @PostMapping("/authorize-user/{userId}")
    public ResponseEntity<String> authorizeUser(@RequestParam UUID adminId,
                                                @PathVariable UUID userId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ApiException("Admin user not found", HttpStatus.NOT_FOUND));
        if (admin.getGlobalRole() != GlobalRole.ADMIN) {
            throw new ApiException("Only ADMIN can authorize users", HttpStatus.FORBIDDEN);
        }
        userService.authorizeUser(userId);
        return ResponseEntity.ok("User authorized successfully");
    }

    /**
     * Retrieves information about a specific user by their ID.
     * 
     * @param userId The ID of the user to retrieve
     * @return ResponseEntity containing the user information
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        
        UserDTO userDTO = new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getEmail(),
                user.getGlobalRole(),
                null
        );
        
        return ResponseEntity.ok(userDTO);
    }
}

package tech.project.schedule.services;

import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.dto.auth.LoginRequest;
import tech.project.schedule.dto.auth.RegistrationRequest;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.enums.UserStatus;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;

import java.util.UUID;


@Service
@RequiredArgsConstructor


public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    /**
     * Base URL for the frontend password reset page, injected from application.properties or environment.
     */
    @Value("${frontend.reset.base-url:http://localhost:4200}")
    private String frontendResetBaseUrl;

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new ApiException("User not found with provided email",
                        HttpStatus.NOT_FOUND));
    }

    public User login(LoginRequest loginRequest) {
        User user = getUserByEmail(loginRequest.email());
        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new ApiException("Incorrect Password.", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * Sets a new password for the user identified by email.
     * @param email The email of the user whose password is to be set
     * @param newPassword The new password to set (plain text)
     * @throws ApiException if user is not found
     */
    @Transactional
    public void setPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found with provided email", HttpStatus.NOT_FOUND));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUserStatus(tech.project.schedule.model.enums.UserStatus.AUTHORIZED);
        userRepository.save(user);
    }


    /**
     * Blocks (sets status to BLOCKED) a user by email.
     * Only to be called by admin.
     * @param email The email of the user to block
     * @throws ApiException if user is not found
     */
    @Transactional
    public void blockUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found with provided email", HttpStatus.NOT_FOUND));
        user.setUserStatus(tech.project.schedule.model.enums.UserStatus.BLOCKED);
        userRepository.save(user);
    }

    public String register(RegistrationRequest request) {

        boolean emailExists = userRepository.findByEmail(request.email()).isPresent();
        if (emailExists) {
            throw new ApiException("User already exists.", HttpStatus.CONFLICT);
        }
        // Generated random password
        String randomPassword = java.util.UUID.randomUUID().toString();
        User user = new User(
                request.firstName(),
                request.lastName(),
                request.email(),
                passwordEncoder.encode(randomPassword),
                request.username()
        );
        user.setGlobalRole(request.role());
        userRepository.save(user);
        /*
          Send email with a password reset link to the newly registered user.
          The user will be able to set their own password after clicking the link.
          The link base is injected from application.properties or environment.
         */
        String resetLink = frontendResetBaseUrl + "/set-password?email=" + user.getEmail();
        String mailText = String.format(MailContent.REGISTRATION_BODY, resetLink);
        mailService.sendRegistrationConfirmation(user.getEmail(), MailContent.REGISTRATION_SUBJECT, mailText);
        return "User registered. Confirmation email sent.";
    }

    /**
     * Sends a password reset email to the given user.
     * The email contains a link to the frontend set-password page with the user's email as a parameter.
     */
    public void sendPasswordResetEmail(User user) {
        String resetLink = frontendResetBaseUrl + "/set-password?email=" + user.getEmail();
        String mailText = String.format(MailContent.PASSWORD_RESET_BODY, resetLink);
        mailService.sendRegistrationConfirmation(user.getEmail(), MailContent.PASSWORD_RESET_SUBJECT, mailText);
    }

    @Transactional
    public void blockUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ApiException("User not found with provided id",
                        HttpStatus.NOT_FOUND));
        user.setUserStatus(UserStatus.BLOCKED);
        userRepository.save(user);
    }

    @Transactional
    public void authorizeUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ApiException("User not found with provided id",
                        HttpStatus.NOT_FOUND));
        user.setUserStatus(UserStatus.AUTHORIZED);
        userRepository.save(user);
    }
}

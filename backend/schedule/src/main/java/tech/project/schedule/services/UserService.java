package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.project.schedule.dto.auth.LoginRequest;
import tech.project.schedule.dto.auth.RegistrationRequest;
import tech.project.schedule.exception.ApiException;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;

/**
 * Service class for managing user authentication and registration.
 * Handles user login verification, new user registration, and implements
 * security measures like password encryption.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    // ToDo: add JWT tokens
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Retrieves a user by their email address.
     * 
     * @param email The email address to search for
     * @return The user entity if found
     * @throws ApiException if no user exists with the provided email
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new ApiException("User not found with provided email",
                        HttpStatus.NOT_FOUND));
    }

     /**
     * Authenticates a user based on email and password.
     * Verifies that the password matches the stored hash.
     * 
     * @param loginRequest DTO containing login credentials
     * @return The authenticated user entity
     * @throws ApiException if user not found or password is incorrect
     */
    public User login(LoginRequest loginRequest) {
        User user = getUserByEmail(loginRequest.getEmail());
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new ApiException("Incorrect Password.", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * Registers a new user in the system.
     * Validates that the email is not already in use and securely hashes the password.
     * 
     * @param request DTO containing registration information
     * @return Success message if registration succeeds
     * @throws ApiException if a user with the provided email already exists
     */
    @Transactional
    public String register(RegistrationRequest request) {
        //ToDO: email validation, sending confirmation email

        boolean emailExists = userRepository.findByEmail(request.getEmail()).isPresent();
        if (emailExists) {
            throw new ApiException("User already exists.", HttpStatus.CONFLICT);
        }
        User user = new User(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getUsername()
        );
        userRepository.save(user);
        return "Registered successfully";
    }
}

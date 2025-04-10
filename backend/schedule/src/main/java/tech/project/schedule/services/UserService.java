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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                ()-> new ApiException("User not found with provided email",
                HttpStatus.NOT_FOUND));
    }

    public User login(LoginRequest loginRequest) {
        User user = getUserByEmail(loginRequest.getEmail());
        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new ApiException("Incorrect Password.", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }

    @Transactional
    public String register(RegistrationRequest request) {
        //ToDO: email validation, sending confirmation email

        boolean emailExists = userRepository.findByEmail(request.getEmail()).isPresent();
        if(emailExists) {
            throw new ApiException("User already exists.", HttpStatus.CONFLICT);
        }
        User user = new User(
                request.getUsername(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );
        userRepository.save(user);
        return "Registered successfully";
    }
}

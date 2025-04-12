package tech.project.schedule.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import tech.project.schedule.dto.auth.LoginRequest;
import tech.project.schedule.dto.auth.LoginResponseDTO;
import tech.project.schedule.dto.auth.RegistrationRequest;
import tech.project.schedule.dto.auth.RegistrationResponseDTO;
import tech.project.schedule.model.user.User;
import tech.project.schedule.services.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

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
}

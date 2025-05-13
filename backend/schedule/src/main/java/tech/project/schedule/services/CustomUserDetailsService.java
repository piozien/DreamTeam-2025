package tech.project.schedule.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;

/**
 * Service that bridges the application's user model with Spring Security's authentication system.
 * Implements Spring Security's UserDetailsService to provide user authentication information
 * from the application's database during the login process.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    /**
     * Loads a user's security details by their email address.
     * Called by Spring Security during authentication to retrieve user information
     * and create a UserDetails object containing credentials and authorities.
     * 
     * @param email The email address used as the username during login
     * @return A Spring Security UserDetails object with authentication information
     * @throws UsernameNotFoundException if no user exists with the provided email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getGlobalRole().name())
                .build();
    }
}

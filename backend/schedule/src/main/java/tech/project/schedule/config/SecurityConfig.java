package tech.project.schedule.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tech.project.schedule.security.JwtAuthenticationFilter;

/**
 * Security configuration class for the scheduling application.
 * Configures Spring Security settings including authentication mechanisms,
 * JWT-based authorization, password encoding, CORS policies, and endpoint access rules.
 * 
 * This class implements WebMvcConfigurer to customize aspects of Spring MVC beyond security,
 * particularly Cross-Origin Resource Sharing (CORS) configuration.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {
      /**
     * Bean for password encoding using BCrypt. Required for injecting into UserService.
     * 
     * @return A BCryptPasswordEncoder instance for secure password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

     /**
     * Configures the security filter chain for HTTP requests.
     * Defines security rules including:
     * - Disabled CSRF protection (as we're using JWT)
     * - Session management policy
     * - Public endpoint allowlist (login, register, password reset)
     * - Authentication requirements for all other endpoints
     * - OAuth2 login configuration with a success handler
     * - JWT authentication filter integration
     * 
     * @param http The HttpSecurity object to configure
     * @return The built SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy
                    (SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/request-password-reset",
                        "/api/auth/set-password", "/login**", "/error**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler((request, response, authentication) -> {
                    response.sendRedirect("/api/auth/oauth2-success");
                })
            )
            .logout(logout -> logout.logoutSuccessUrl("/"))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

     /**
     * Configures Cross-Origin Resource Sharing (CORS) for the application.
     * Allows the frontend application (running on localhost:4200) to make API requests
     * to this backend service by specifying allowed origins, methods, headers, and credentials.
     * 
     * @param registry The CorsRegistry to configure
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "*")
                .allowCredentials(true);
    }

    /**
     * Creates and configures the AuthenticationManager bean.
     * Sets up the user details service and password encoder to be used for authentication.
     * 
     * @param http The HttpSecurity object to extract the shared AuthenticationManagerBuilder
     * @return The configured AuthenticationManager
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return builder.build();
    }
}

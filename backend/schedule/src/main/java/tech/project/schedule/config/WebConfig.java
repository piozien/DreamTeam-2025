package tech.project.schedule.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.security.config.Customizer;

/**
 * Configuration class for web security and cross-origin resource sharing (CORS).
 * Sets up security filters, password encoding, and defines allowed origins for CORS.
 * This configuration is designed for development purposes with minimal security restrictions.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

     /**
     * Creates a password encoder bean for securely hashing passwords.
     * 
     * @return A BCryptPasswordEncoder instance
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

     /**
     * Configures the security filter chain.
     * Currently set to disable CSRF protection and allow all requests.
     * 
     * @param http The HttpSecurity to configure
     * @return The built SecurityFilterChain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .cors(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Configures CORS settings to allow cross-origin requests from the frontend application.
     * 
     * @param registry The CorsRegistry to configure
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}

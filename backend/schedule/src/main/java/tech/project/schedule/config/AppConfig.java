package tech.project.schedule.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for application-wide beans and settings.
 * Defines common beans that can be used across the application.
 */
@Configuration
public class AppConfig {

    /**
     * Creates a RestTemplate bean for making HTTP requests.
     * Used by components like NotificationClient to communicate with APIs.
     *
     * @return A configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 
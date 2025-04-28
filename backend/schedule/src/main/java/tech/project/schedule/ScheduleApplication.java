package tech.project.schedule;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class that serves as the entry point for the scheduling system.
 * 
 * This class bootstraps the Spring Boot application, initializing the Spring context
 * and starting all configured components of the task and project scheduling system.
 * The @SpringBootApplication annotation enables auto-configuration, component scanning,
 * and defines this class as a configuration source.
 */
@SpringBootApplication
public class ScheduleApplication {

	/**
     * Main method that launches the Spring Boot application.
     * Creates the application context and starts the embedded web server.
     *
     * @param args Command line arguments passed to the application
     */
	public static void main(String[] args) {
		SpringApplication.run(ScheduleApplication.class, args);
	}

}


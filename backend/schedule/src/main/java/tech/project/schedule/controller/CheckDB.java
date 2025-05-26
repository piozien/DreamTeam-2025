package tech.project.schedule.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Controller that provides an endpoint to verify database connectivity.
 * This is useful for health checking and troubleshooting database connection issues.
 */
@RestController
public class CheckDB {
    
    /**
     * DataSource that provides connections to the configured database.
     * Automatically injected by Spring's dependency injection.
     */
    @Autowired
    private DataSource dataSource;
    
     /**
     * Verifies the application's ability to connect to the database.
     * 
     * @return A string message indicating either successful connection (with database URL) 
     *         or connection failure details
     */
    @GetMapping("/check-db")
    public String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return "Connected to: " + connection.getMetaData().getURL();
        } catch (SQLException e) {
            return "Database connection failed: " + e.getMessage();
        }
    }
}

package tech.project.schedule.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
public class CheckDB {
    @Autowired
    private DataSource dataSource;

    @GetMapping("/check-db")
    public String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return "Connected to: " + connection.getMetaData().getURL();
        } catch (SQLException e) {
            return "Database connection failed: " + e.getMessage();
        }
    }
}

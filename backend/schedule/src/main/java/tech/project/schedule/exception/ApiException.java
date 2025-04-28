package tech.project.schedule.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom exception class for handling API-specific exceptions throughout the application.
 * Provides functionality to include HTTP status codes with exception messages,
 * allowing for more detailed error information to be returned to clients.
 */
@Getter
public class ApiException extends RuntimeException {
    private HttpStatus status;
    private final String message;

     /**
     * Constructs an ApiException with a specified error message and HTTP status.
     * 
     * @param message The detail message explaining the exception
     * @param status The HTTP status code to be returned to the client
     */
    public ApiException(String message, HttpStatus status) {
        super(message);
        this.message = message;
        this.status = status;

    }
    
    /**
     * Constructs an ApiException with only an error message.
     * Use this constructor when the HTTP status is not relevant or will be set elsewhere.
     * 
     * @param message The detail message explaining the exception
     */
    public ApiException(String message) {
        super(message);
        this.message = message;

    }
}

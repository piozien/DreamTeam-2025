package tech.project.schedule.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


/**
 * Global exception handler for centralized exception processing across the application.
 * Intercepts exceptions thrown during request processing and converts them to
 * standardized API responses with appropriate HTTP status codes and error details.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

      /**
     * Handles ApiException instances by converting them into structured error responses.
     * Creates a consistent error response format with timestamp, status code, error type,
     * error message, and request path information.
     *
     * @param ex The ApiException that was thrown
     * @param request The web request during which the exception was thrown
     * @return ResponseEntity containing error details and appropriate HTTP status code
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", ex.getStatus().value());
        body.put("error", ex.getStatus().getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));
        
        return new ResponseEntity<>(body, ex.getStatus());
    }
}

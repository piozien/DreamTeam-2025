package tech.project.schedule.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.project.schedule.dto.calendar.EventDTO;
import tech.project.schedule.model.user.User;
import tech.project.schedule.repositories.UserRepository;
import tech.project.schedule.services.GoogleCalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST controller that handles integration with Google Calendar.
 * Provides endpoints for creating, updating, and deleting calendar events through
 * Google's API. Uses user authentication to manage calendar access tokens.
 */
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class GoogleCalendarController {

    private final GoogleCalendarService calendarService;
    private final UserRepository userRepository;
    
    /**
     * Helper method to extract the current authenticated user's ID
     * 
     * @return UUID of the currently authenticated user
     * @throws RuntimeException if no user is authenticated
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        return user.getId();
    }

    /**
     * Creates a new event in the user's Google Calendar.
     * Uses the authenticated user's ID to find their stored OAuth tokens.
     *
     * @param eventDTO Data transfer object containing event information (title, timing, location, etc.)
     * @return Response with created event ID or error message
     */
    @PostMapping("/create-event")
    public ResponseEntity<String> createEvent(@RequestBody EventDTO eventDTO) {
        try {
            UUID userId = getCurrentUserId();
            String eventId = calendarService.createEvent(userId, eventDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(eventId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Failed to create event: " + e.getMessage());
        }
    }
    // Evil attempt to fix date problem boss said

    /**
     * Test endpoint for validating date/time parsing and timezone conversions.
     * Attempts to fix date formatting issues by explicitly setting timezone.
     *
     * @param dateTime String representation of date/time in format "dd.MM.yyyy:HH:mm:ss"
     * @return Response with the parsed and timezone-adjusted datetime
     */
    @PostMapping("/test-datetime")
    public ResponseEntity<String> testDateTime(@RequestParam String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);

        ZonedDateTime adjustedDateTime = localDateTime.atZone(ZoneId.of("Europe/Warsaw"));

        return ResponseEntity.ok("Adjusted DateTime: " + adjustedDateTime);
    }

    /**
     * Updates an existing event in the user's Google Calendar.
     *
     * @param eventId  The unique identifier for the Google Calendar event to update
     * @param eventDTO Data transfer object containing updated event details
     * @return Response with success message or error details
     */
    @PutMapping("/update-event/{eventId}")
    public ResponseEntity<String> updateEvent(
            @PathVariable String eventId,
            @RequestBody EventDTO eventDTO) {
        try {
            UUID userId = getCurrentUserId();
            String response = calendarService.updateEvent(userId, eventId, eventDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body("Failed to update event: " + e.getMessage());
        }
    }

    /**
     * Deletes an event from the user's Google Calendar.
     *
     * @param eventId The unique identifier for the Google Calendar event to delete
     * @return Empty response with appropriate status code (204 for success, 500 for error)
     */
    @DeleteMapping("/delete-event/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String eventId) {
        try {
            UUID userId = getCurrentUserId();
            calendarService.deleteEvent(userId, eventId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}


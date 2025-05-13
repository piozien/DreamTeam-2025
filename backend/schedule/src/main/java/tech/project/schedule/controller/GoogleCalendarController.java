package tech.project.schedule.controller;

import tech.project.schedule.dto.calendar.EventDTO;
import tech.project.schedule.services.GoogleCalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * REST controller that handles integration with Google Calendar.
 * Provides endpoints for creating, updating, and deleting calendar events through
 * Google's API. Uses OAuth2 for authentication with Google services.
 */
@RestController
@RequestMapping("/api/calendar")
public class GoogleCalendarController {

    private final GoogleCalendarService calendarService;

    
    /**
     * Constructor with dependency injection for the Google Calendar service.
     * 
     * @param calendarService Service that handles actual communication with Google Calendar API
     */
    public GoogleCalendarController(GoogleCalendarService calendarService) {
        this.calendarService = calendarService;
    }

     /**
     * Creates a new event in the user's Google Calendar.
     * Uses the authenticated user's OAuth2 token to authorize the request.
     * 
     * @param authorizedClient OAuth2 client with user's Google authorization credentials
     * @param eventDTO Data transfer object containing event information (title, timing, location, etc.)
     * @return Response with created event ID or error message
     */
    @PostMapping("/create-event")
    public ResponseEntity<String> createEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @RequestBody EventDTO eventDTO) {
        try {
            String eventId = calendarService.createEvent(authorizedClient, eventDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(eventId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create event.");
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

        return ResponseEntity.ok("Adjusted DateTime: " + adjustedDateTime.toString());
    }


   
    /**
     * Updates an existing event in the user's Google Calendar.
     * 
     * @param authorizedClient OAuth2 client with user's Google authorization credentials
     * @param eventId The unique identifier for the Google Calendar event to update
     * @param eventDTO Data transfer object containing updated event details
     * @return Response with success message or error details
     */
    @PutMapping("/update-event/{eventId}")
    public ResponseEntity<String> updateEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String eventId,
            @RequestBody EventDTO eventDTO) {
        try {
            String response = calendarService.updateEvent(authorizedClient, eventId, eventDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update event.");
        }
    }

    /**
     * Deletes an event from the user's Google Calendar.
     * 
     * @param authorizedClient OAuth2 client with user's Google authorization credentials
     * @param eventId The unique identifier for the Google Calendar event to delete
     * @return Empty response with appropriate status code (204 for success, 500 for error)
     */
    @DeleteMapping("/delete-event/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String eventId) {
        try {
            calendarService.deleteEvent(authorizedClient, eventId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}


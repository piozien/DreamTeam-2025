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
 * REST controller for integrating with Google Calendar.
 * Provides endpoints for creating, updating, and deleting events in a user's
 * Google Calendar using OAuth2 authentication. This integration allows the
 * scheduling application to synchronize project and task dates with users'
 * personal calendars.
 */
@RestController
@RequestMapping("/api/calendar")
public class GoogleCalendarController {

    private final GoogleCalendarService calendarService;

    /**
     * Creates a new GoogleCalendarController with the required service.
     * 
     * @param calendarService The service that handles Google Calendar operations
     */
    public GoogleCalendarController(GoogleCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /**
     * Creates a new event in the user's Google Calendar.
     * Uses the authenticated user's OAuth2 token to authorize the request.
     * 
     * @param authorizedClient The OAuth2 client with authorization details
     * @param eventDTO The event data to create in Google Calendar
     * @return ResponseEntity containing the created event ID or an error message
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
     * Test endpoint for validating datetime parsing and timezone handling.
     * Converts a formatted datetime string to a timezone-adjusted ZonedDateTime.
     * 
     * @param dateTime The datetime string in format "dd.MM.yyyy:HH:mm:ss"
     * @return ResponseEntity with the adjusted datetime string
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
     * Uses the authenticated user's OAuth2 token to authorize the request.
     * 
     * @param authorizedClient The OAuth2 client with authorization details
     * @param eventId The ID of the Google Calendar event to update
     * @param eventDTO The updated event data
     * @return ResponseEntity containing the response from Google Calendar or an error message
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
     * Uses the authenticated user's OAuth2 token to authorize the request.
     * 
     * @param authorizedClient The OAuth2 client with authorization details
     * @param eventId The ID of the Google Calendar event to delete
     * @return ResponseEntity with no content on success, or an error status
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


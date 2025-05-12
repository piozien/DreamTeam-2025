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

@RestController
@RequestMapping("/api/calendar")
public class GoogleCalendarController {

    private final GoogleCalendarService calendarService;

    public GoogleCalendarController(GoogleCalendarService calendarService) {
        this.calendarService = calendarService;
    }

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
    @PostMapping("/test-datetime")
    public ResponseEntity<String> testDateTime(@RequestParam String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);

        ZonedDateTime adjustedDateTime = localDateTime.atZone(ZoneId.of("Europe/Warsaw"));

        return ResponseEntity.ok("Adjusted DateTime: " + adjustedDateTime.toString());
    }


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


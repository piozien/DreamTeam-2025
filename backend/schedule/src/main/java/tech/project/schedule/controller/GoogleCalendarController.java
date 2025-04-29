package tech.project.schedule.controller;

import tech.project.schedule.dto.calendar.EventDTO;
import tech.project.schedule.services.GoogleCalendarService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calendar")
public class GoogleCalendarController {

    private final GoogleCalendarService calendarService;

    public GoogleCalendarController(GoogleCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @PostMapping("/create-event")
    public String createEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @RequestBody EventDTO eventDTO) {
        return calendarService.createEvent(authorizedClient, eventDTO);
    }

    @PutMapping("/update-event/{eventId}")
    public String updateEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String eventId,
            @RequestBody EventDTO eventDTO) {
        return calendarService.updateEvent(authorizedClient, eventId, eventDTO);
    }

    @DeleteMapping("/delete-event/{eventId}")
    public void deleteEvent(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @PathVariable String eventId) {
        calendarService.deleteEvent(authorizedClient, eventId);
    }
}

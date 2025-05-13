package tech.project.schedule.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tech.project.schedule.dto.calendar.EventDTO;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Service for interacting with Google Calendar API.
 * Provides methods to create, update, and delete calendar events using
 * the authenticated user's OAuth2 credentials. Handles the conversion
 * between application data and Google Calendar API requirements.
 */
@Service
public class GoogleCalendarService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String CALENDAR_API_BASE_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);

    /**
     * Creates a new event in the user's primary Google Calendar.
     * 
     * @param authorizedClient OAuth2 client with user's Google API credentials
     * @param eventDTO Event data containing details like title, location, and timing
     * @return The Google Calendar API response containing the created event ID
     * @throws RuntimeException if the API request fails
     */
    public String createEvent(OAuth2AuthorizedClient authorizedClient, EventDTO eventDTO) {
        try {
            HttpHeaders headers = createHeaders(authorizedClient);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(buildEventBody(eventDTO), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    CALENDAR_API_BASE_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error creating event: {}", e.getMessage());
            throw new RuntimeException("Unable to create event in calendar.");
        }
    }


     /**
     * Updates an existing event in the user's Google Calendar.
     * 
     * @param authorizedClient OAuth2 client with user's Google API credentials
     * @param eventId The ID of the event to update
     * @param eventDTO Updated event data
     * @return The Google Calendar API response containing the updated event
     * @throws RuntimeException if the API request fails
     */
    public String updateEvent(OAuth2AuthorizedClient authorizedClient, String eventId, EventDTO eventDTO) {
        try {
            String url = CALENDAR_API_BASE_URL + "/" + eventId;
            HttpHeaders headers = createHeaders(authorizedClient);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(buildEventBody(eventDTO), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            return response.getBody();
        } catch (RestClientException e) {
            logger.error("Error updating calendar event: {}", e.getMessage());
            throw new RuntimeException("Unable to update event in calendar.");
        }
    }

    /**
     * Deletes an event from the user's Google Calendar.
     * 
     * @param authorizedClient OAuth2 client with user's Google API credentials
     * @param eventId The ID of the event to delete
     * @throws RuntimeException if the API request fails
     */
    public void deleteEvent(OAuth2AuthorizedClient authorizedClient, String eventId) {
        try {
            String url = CALENDAR_API_BASE_URL + "/" + eventId;
            HttpHeaders headers = createHeaders(authorizedClient);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );
        } catch (RestClientException e) {
            logger.error("Error deleting event: {}", e.getMessage());
            throw new RuntimeException("Unable to delete event in calendar.");
        }
    }
    
     /**
     * Converts a datetime string to a timezone-aware format.
     * Used to ensure proper timezone handling in calendar events.
     * 
     * @param dateTime String in format "dd.MM.yyyy:HH:mm:ss"
     * @return Timezone-adjusted datetime string
     */
    public String adjustDateTime(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);

        ZonedDateTime adjustedDateTime = localDateTime.atZone(ZoneId.of("Europe/Warsaw"));

        return adjustedDateTime.toString();
    }


    /**
     * Creates HTTP headers with OAuth2 authorization for Google API requests.
     * 
     * @param authorizedClient OAuth2 client containing access token
     * @return Configured HTTP headers
     */
    private HttpHeaders createHeaders(OAuth2AuthorizedClient authorizedClient) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

     /**
     * Builds the event request body in the format required by Google Calendar API.
     * 
     * @param eventDTO Event data from the application
     * @return Map representing the JSON structure for the API request
     */
    private Map<String, Object> buildEventBody(EventDTO eventDTO) {
        Map<String, Object> event = new HashMap<>();
        event.put("summary", eventDTO.summary());
        event.put("location", eventDTO.location());
        event.put("description", eventDTO.description());

        Map<String, Object> start = new HashMap<>();
        start.put("dateTime", eventDTO.startDateTime());
        start.put("timeZone", eventDTO.timeZone());
        event.put("start", start);

        Map<String, Object> end = new HashMap<>();
        end.put("dateTime", eventDTO.endDateTime());
        end.put("timeZone", eventDTO.timeZone());
        event.put("end", end);

        return event;
    }

}

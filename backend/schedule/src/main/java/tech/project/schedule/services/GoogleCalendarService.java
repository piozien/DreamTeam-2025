package tech.project.schedule.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
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
import java.util.UUID;


/**
 * Service for interacting with Google Calendar API.
 * Provides methods to create, update, and delete calendar events using
 * the authenticated user's OAuth2 credentials. Handles the conversion
 * between application data and Google Calendar API requirements.
 */
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final OAuth2TokenService tokenService;
    
    private static final String CALENDAR_API_BASE_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);

    /**
     * Creates a new event in the user's primary Google Calendar.
     * 
     * @param userId UUID of the user for whom to create the event
     * @param eventDTO Event data containing details like title, location, and timing
     * @return The Google Calendar API response containing the created event ID
     * @throws RuntimeException if the API request fails or token is not available
     */
    public String createEvent(UUID userId, EventDTO eventDTO) {
        try {
            log.info("Creating calendar event for user: {}", userId);
            log.info("Event details: summary={}, start={}, end={}, timeZone={}", 
                    eventDTO.summary(), eventDTO.startDateTime(), eventDTO.endDateTime(), eventDTO.timeZone());
            
            String accessToken = tokenService.getAccessToken(userId);
            if (accessToken == null) {
                log.error("No access token available for user: {}", userId);
                throw new RuntimeException("No access token available for user. Please authorize Google Calendar access.");
            }
            
            HttpHeaders headers = createHeaders(accessToken);
            Map<String, Object> eventBody = buildEventBody(eventDTO);
            log.info("Request payload: {}", eventBody);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(eventBody, headers);

            log.info("Sending request to Google Calendar API: {}", CALENDAR_API_BASE_URL);
            ResponseEntity<String> response = restTemplate.exchange(
                    CALENDAR_API_BASE_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("Event created successfully with response: {}", response.getStatusCode());
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error creating event: {}", e.getMessage());
            log.error("Exception details: ", e);
            throw new RuntimeException("Unable to create event in calendar: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating event: {}", e.getMessage());
            log.error("Exception details: ", e);
            throw new RuntimeException("Unexpected error creating event: " + e.getMessage());
        }
    }


    /**
     * Updates an existing event in the user's Google Calendar.
     * 
     * @param userId UUID of the user who owns the event
     * @param eventId The ID of the event to update
     * @param eventDTO Updated event data
     * @return The Google Calendar API response containing the updated event
     * @throws RuntimeException if the API request fails or token is not available
     */
    public String updateEvent(UUID userId, String eventId, EventDTO eventDTO) {
        try {
            String accessToken = tokenService.getAccessToken(userId);
            if (accessToken == null) {
                throw new RuntimeException("No access token available for user. Please authorize Google Calendar access.");
            }
            
            String url = CALENDAR_API_BASE_URL + "/" + eventId;
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(buildEventBody(eventDTO), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error updating calendar event: {}", e.getMessage());
            throw new RuntimeException("Unable to update event in calendar.");
        }
    }

    /**
     * Deletes an event from the user's Google Calendar.
     * 
     * @param userId UUID of the user who owns the event
     * @param eventId The ID of the event to delete
     * @throws RuntimeException if the API request fails or token is not available
     */
    public void deleteEvent(UUID userId, String eventId) {
        try {
            String accessToken = tokenService.getAccessToken(userId);
            if (accessToken == null) {
                throw new RuntimeException("No access token available for user. Please authorize Google Calendar access.");
            }
            
            String url = CALENDAR_API_BASE_URL + "/" + eventId;
            HttpHeaders headers = createHeaders(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );
        } catch (RestClientException e) {
            log.error("Error deleting event: {}", e.getMessage());
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
     * @param accessToken OAuth2 access token
     * @return Configured HTTP headers
     */
    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Convert date string from dd.MM.yyyy:HH:mm:ss format to ISO-8601 format required by Google Calendar API.
     * If the date is already in ISO-8601 format, return it as is.
     * 
     * @param dateTime The date string to convert
     * @param timeZone The time zone to use
     * @return ISO-8601 formatted date string
     */
    private String formatDateToISO8601(String dateTime, String timeZone) {
        try {
            // Check if date is already in ISO-8601 format
            if (dateTime.contains("T") && dateTime.contains("-")) {
                return dateTime; // Already in correct format
            }
            
            // Parse the input date format (dd.MM.yyyy:HH:mm:ss)
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm:ss");
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime, inputFormatter);
            
            // Convert to ZonedDateTime with the specified time zone
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timeZone));
            
            // Format as ISO-8601 (format required by Google Calendar API)
            return zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', returning as is: {}", dateTime, e.getMessage());
            return dateTime; // Return original on error
        }
    }
    
    private Map<String, Object> buildEventBody(EventDTO eventDTO) {
        Map<String, Object> event = new HashMap<>();
        event.put("summary", eventDTO.summary());
        //event.put("location", eventDTO.location());
        event.put("description", eventDTO.description());

        Map<String, Object> start = new HashMap<>();
        start.put("dateTime", formatDateToISO8601(eventDTO.startDateTime(), eventDTO.timeZone()));
        start.put("timeZone", eventDTO.timeZone());
        event.put("start", start);

        Map<String, Object> end = new HashMap<>();
        end.put("dateTime", formatDateToISO8601(eventDTO.endDateTime(), eventDTO.timeZone()));
        end.put("timeZone", eventDTO.timeZone());
        event.put("end", end);

        return event;
    }

}

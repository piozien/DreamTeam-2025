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


@Service
public class GoogleCalendarService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String CALENDAR_API_BASE_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);

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
    //  Convert Locale Date time to Zone
    public String adjustDateTime(String dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTime, formatter);

        ZonedDateTime adjustedDateTime = localDateTime.atZone(ZoneId.of("Europe/Warsaw"));

        return adjustedDateTime.toString();
    }


    private HttpHeaders createHeaders(OAuth2AuthorizedClient authorizedClient) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

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

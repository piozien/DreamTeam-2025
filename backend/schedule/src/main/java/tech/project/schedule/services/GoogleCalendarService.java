package tech.project.schedule.services;

import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tech.project.schedule.dto.calendar.EventDTO;

import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleCalendarService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String CALENDAR_API_BASE_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

    public String createEvent(OAuth2AuthorizedClient authorizedClient, EventDTO eventDTO) {
        HttpHeaders headers = createHeaders(authorizedClient);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(buildEventBody(eventDTO), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                CALENDAR_API_BASE_URL,
                HttpMethod.POST,
                request,
                String.class
        );

        return response.getBody();
    }

    public String updateEvent(OAuth2AuthorizedClient authorizedClient, String eventId, EventDTO eventDTO) {
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
    }

    public void deleteEvent(OAuth2AuthorizedClient authorizedClient, String eventId) {
        String url = CALENDAR_API_BASE_URL + "/" + eventId;
        HttpHeaders headers = createHeaders(authorizedClient);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                request,
                Void.class
        );
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

package tech.project.schedule.services;

import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleCalendarService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String createEvent(OAuth2AuthorizedClient authorizedClient) {
        String url = "https://www.googleapis.com/calendar/v3/calendars/primary/events";

        Map<String, Object> event = new HashMap<>();
        event.put("summary", "New Event");
        event.put("description", "Description");

        Map<String, Object> start = new HashMap<>();
        start.put("dateTime", "2025-04-28T10:00:00-07:00");
        event.put("start", start);

        Map<String, Object> end = new HashMap<>();
        end.put("dateTime", "2025-04-28T11:00:00-07:00");
        event.put("end", end);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(event, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
        );

        return response.getBody();
    }
}

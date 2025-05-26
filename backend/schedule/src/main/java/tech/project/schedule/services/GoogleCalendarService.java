package tech.project.schedule.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.project.schedule.dto.calendar.EventDTO;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service for interacting with Google Calendar API using the official Java client library.
 * Provides methods to create and manage a dedicated team calendar, and handle event operations.
 */
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final OAuth2TokenService tokenService;
    
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String APPLICATION_NAME = "DreamTeam Calendar";
    private static final String TIMEZONE = "Europe/Warsaw";
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/calendar");
    
    @Value("${google.calendar.team-calendar-id:}")
    private String teamCalendarId;
    
    @Value("${GOOGLE_SERVICE_ACCOUNT:}")
    private String serviceAccountEmail;
    
    @Value("${SERVICE_ACCOUNT_KEY_PATH:serviceEmail.json}")
    private String serviceAccountKeyPath;
    
    /**
     * Creates a Calendar service instance for a specific user
     * 
     * @param userId The user ID to create the calendar service for
     * @return A configured Calendar service instance
     * @throws IOException If there's an error with authentication
     * @throws GeneralSecurityException If there's a security error
     */
    private Calendar getCalendarService(UUID userId) throws IOException, GeneralSecurityException {
        String accessToken = tokenService.getAccessToken(userId);
        if (accessToken == null) {
            throw new IOException("No access token available for user: " + userId);
        }
        
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        
        return new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
    
    /**
     * Creates a Calendar service instance using the service account credentials.
     * This allows the application to manage calendar events without user-specific OAuth tokens.
     * 
     * @return A configured Calendar service instance with service account credentials
     * @throws IOException If there's an error reading the service account key file
     * @throws GeneralSecurityException If there's a security error with the credentials
     */
    private Calendar getServiceAccountCalendarService() throws IOException, GeneralSecurityException {
        log.info("Creating calendar service with service account: {}", serviceAccountEmail);
        
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            
            File keyFile = new File(serviceAccountKeyPath);
            if (!keyFile.exists()) {
                throw new FileNotFoundException("Service account key file not found: " + serviceAccountKeyPath);
            }
            
            GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(keyFile))
                    .createScoped(SCOPES);
            
            Calendar service = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
                    
            return service;
        } catch (Exception e) {
            log.error("Error creating service account calendar service: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Creates a new team calendar with the specified name and timezone
     * 
     * @param userId The user ID with admin privileges
     * @return The ID of the created calendar
     * @throws IOException If there's an error creating the calendar
     * @throws GeneralSecurityException If there's a security error
     */
    public String createTeamCalendar(UUID userId) throws IOException, GeneralSecurityException {
        Calendar service = getCalendarService(userId);
        
        // Check if team calendar ID is already set
        if (teamCalendarId != null && !teamCalendarId.isEmpty()) {
            try {
                // Verify the calendar exists
                service.calendars().get(teamCalendarId).execute();
                log.info("Team calendar already exists with ID: {}", teamCalendarId);
                return teamCalendarId;
            } catch (IOException e) {
                log.warn("Stored calendar ID is invalid. Creating a new calendar.");
            }
        }
        
        // Create a new calendar
        com.google.api.services.calendar.model.Calendar calendar = 
                new com.google.api.services.calendar.model.Calendar();
        calendar.setSummary("DreamTeam");
        calendar.setTimeZone(TIMEZONE);
        
        try {
            calendar = service.calendars().insert(calendar).execute();
            String newCalendarId = calendar.getId();
            log.info("Created new team calendar with ID: {}", newCalendarId);
            
            // Save the calendar ID
            saveCalendarId(newCalendarId);
            
            return newCalendarId;
        } catch (IOException e) {
            log.error("Error creating team calendar: {}", e.getMessage());
            throw e;
        }
    }
    
    // Service account is already manually added to the calendar, no need to create a new one
    
    /**
     * Save the calendar ID to a properties file for future use
     * 
     * @param calendarId The calendar ID to save
     */
    private void saveCalendarId(String calendarId) {
        try {
            // First try to save to application.properties if it exists
            Path propertiesPath = Paths.get("src/main/resources/application.properties");
            if (Files.exists(propertiesPath)) {
                List<String> lines = Files.readAllLines(propertiesPath);
                boolean found = false;
                
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).startsWith("google.calendar.team-calendar-id=")) {
                        lines.set(i, "google.calendar.team-calendar-id=" + calendarId);
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    lines.add("google.calendar.team-calendar-id=" + calendarId);
                }
                
                Files.write(propertiesPath, lines);
                log.info("Saved calendar ID to application.properties");
            } else {
                // Save to a dedicated file if application.properties doesn't exist
                Path calendarConfigPath = Paths.get("calendar-config.properties");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(calendarConfigPath.toFile()))) {
                    writer.write("teamCalendarId=" + calendarId);
                }
                log.info("Saved calendar ID to calendar-config.properties");
            }
            
            // Update the current instance
            this.teamCalendarId = calendarId;
        } catch (IOException e) {
            log.error("Error saving calendar ID: {}", e.getMessage());
        }
    }
    
    /**
     * Get the team calendar ID - creates a new calendar if necessary
     * 
     * @param userId The user ID with admin privileges
     * @return The team calendar ID
     * @throws IOException If there's an error with the calendar
     * @throws GeneralSecurityException If there's a security error
     */
    public String getTeamCalendarId(UUID userId) throws IOException, GeneralSecurityException {
        if (teamCalendarId == null || teamCalendarId.isEmpty()) {
            return createTeamCalendar(userId);
        }
        return teamCalendarId;
    }
    
    /**
     * Get the team calendar ID for service account operations
     * 
     * @return The team calendar ID
     * @throws IOException If there's an error with the calendar
     */
    public String getTeamCalendarIdWithServiceAccount() throws IOException {
        if (teamCalendarId == null || teamCalendarId.isEmpty()) {
            throw new IOException("No team calendar ID available. Please set up the calendar ID first.");
        }
        return teamCalendarId;
    }
    
    /**
     * Creates a new event in the team calendar for a task
     * 
     * @param adminUserId Admin user ID to access the calendar
     * @param summary Event summary/title
     * @param start Start time
     * @param end End time
     * @param userEmail Email of the user to add as attendee
     * @return The created event
     * @throws IOException If there's an error creating the event
     * @throws GeneralSecurityException If there's a security error
     */
    public Event createTaskEvent(UUID adminUserId, String summary, ZonedDateTime start, 
                               ZonedDateTime end, String userEmail) throws IOException, GeneralSecurityException {
        Calendar service = getCalendarService(adminUserId);
        String calendarId = getTeamCalendarId(adminUserId);

        ZonedDateTime warsawStart = start.withZoneSameInstant(ZoneId.of(TIMEZONE));
        ZonedDateTime warsawEnd = end.withZoneSameInstant(ZoneId.of(TIMEZONE));
        
        log.info("Creating task event: {} from {} to {}", summary, warsawStart, warsawEnd);
        
        DateTime startDateTime = new DateTime(warsawStart.toInstant().toEpochMilli());
        DateTime endDateTime = new DateTime(warsawEnd.toInstant().toEpochMilli());
        
        log.info("Start DateTime: {}, End DateTime: {}", startDateTime.toStringRfc3339(), endDateTime.toStringRfc3339());
        
        Event event = new Event()
            .setSummary(summary)
            .setDescription("Zadanie z aplikacji DreamTeam")
            .setStart(new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(TIMEZONE))
            .setEnd(new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(TIMEZONE))
            .setVisibility("default")
            .set("guestsCanSeeOtherGuests", true);
        
        if (userEmail != null && !userEmail.isEmpty()) {
            EventAttendee attendee = new EventAttendee()
                .setEmail(userEmail);
            
            event.setAttendees(Collections.singletonList(attendee));
        }
        
        return service.events().insert(calendarId, event).execute();
    }
    
    /**
     * Creates a new event in the team calendar for a task using the service account
     * Since service accounts cannot add attendees without Domain-Wide Delegation,
     * we'll include assignee information in the description instead.
     * 
     * @param summary Event summary/title
     * @param start Start time
     * @param end End time
     * @param userEmail Email of the user to add as assignee (will be added to description)
     * @param userName Name of the user to add as assignee (will be added to description)
     * @return The created event
     * @throws IOException If there's an error creating the event
     * @throws GeneralSecurityException If there's a security error
     */
    public Event createTaskEventWithServiceAccount(String summary, ZonedDateTime start, 
                               ZonedDateTime end, String userEmail, String userName) throws IOException, GeneralSecurityException {
        Calendar service = getServiceAccountCalendarService();
        String calendarId = getTeamCalendarIdWithServiceAccount();
        
        if (calendarId == null || calendarId.isEmpty()) {
            throw new IllegalStateException("Calendar ID is not set. Please configure the GOOGLE_TEAM_CALENDAR_ID environment variable.");
        }
        
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates must be specified for calendar events");
        }
        
        ZonedDateTime warsawStart = start.withZoneSameInstant(ZoneId.of(TIMEZONE));
        ZonedDateTime warsawEnd = end.withZoneSameInstant(ZoneId.of(TIMEZONE));
        
        log.info("Creating task event with service account: {} from {} to {}", summary, warsawStart, warsawEnd);
        
        DateTime startDateTime = new DateTime(warsawStart.toInstant().toEpochMilli());
        DateTime endDateTime = new DateTime(warsawEnd.toInstant().toEpochMilli());
        
        // Create description with assignee information
        String description = "Zadanie z aplikacji DreamTeam\n\n";
        if (userEmail != null && !userEmail.isEmpty()) {
            description += "Przypisany do: " + userName + " (" + userEmail + ")";
        }
        
        Event event = new Event()
            .setSummary(summary)
            .setDescription(description)
            .setStart(new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone(TIMEZONE))
            .setEnd(new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone(TIMEZONE))
            .setVisibility("default");
        
        // Don't set attendees with service account as it requires Domain-Wide Delegation
        
        try {
            Event createdEvent = service.events().insert(calendarId, event).execute();
            log.info("Event created successfully with ID: {}", createdEvent.getId());
            return createdEvent;
        } catch (Exception e) {
            log.error("Error inserting event: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Updates an event description to include a new assignee using the service account
     * Since service accounts cannot add attendees without Domain-Wide Delegation,
     * we'll update the description to include the new assignee instead.
     * 
     * @param eventId The event ID
     * @param userEmail Email of the user to add to description
     * @param userName Name of the user to add to description
     * @return The updated event
     * @throws IOException If there's an error updating the event
     * @throws GeneralSecurityException If there's a security error
     */
    public Event updateEventDescriptionWithAssignee(String eventId, String userEmail, String userName) 
            throws IOException, GeneralSecurityException {
        Calendar service = getServiceAccountCalendarService();
        String calendarId = getTeamCalendarIdWithServiceAccount();

        Event event = service.events().get(calendarId, eventId).execute();

        log.info("Adding user {} to event {} description with service account", userEmail, eventId);

        // Get current description or create a new one
        String description = event.getDescription();
        if (description == null) {
            description = "Zadanie z aplikacji DreamTeam\n\n";
        }
        
        // Check if user is already in the description
        if (!description.contains(userEmail)) {
            // Add the new assignee to the description
            if (!description.contains("\nPrzypisany do:")) {
                description += "\n\nPrzypisany do: " + userName + " (" + userEmail + ")";
            } else {
                description += ", " + userName + " (" + userEmail + ")";
            }
            
            event.setDescription(description);
            
            return service.events().update(calendarId, eventId, event).execute();
        } else {
            log.info("User {} is already in the description of event {}", userEmail, eventId);
            return event;
        }
    }
    
    /**
     * Removes an attendee from an event
     * 
     * @param adminUserId Admin user ID to access the calendar
     * @param eventId The event ID
     * @param userEmail Email of the user to remove
     * @return The updated event
     * @throws IOException If there's an error updating the event
     * @throws GeneralSecurityException If there's a security error
     */
    public Event removeAttendeeFromEvent(UUID adminUserId, String eventId, String userEmail) 
            throws IOException, GeneralSecurityException {
        Calendar service = getCalendarService(adminUserId);
        String calendarId = getTeamCalendarId(adminUserId);
        
        // Get the current event
        Event event = service.events().get(calendarId, eventId).execute();
        
        // Remove the attendee
        List<EventAttendee> attendees = event.getAttendees();
        if (attendees != null) {
            attendees.removeIf(a -> a.getEmail().equals(userEmail));
            event.setAttendees(attendees);
            
            // Update the event
            return service.events().update(calendarId, eventId, event).execute();
        }
        
        return event;
    }
    
    /**
     * Updates an event description to remove an assignee using the service account
     * Since service accounts cannot manage attendees without Domain-Wide Delegation,
     * we'll update the description to remove the assignee instead.
     * 
     * @param eventId The event ID
     * @param userEmail Email of the user to remove from description
     * @return The updated event
     * @throws IOException If there's an error updating the event
     * @throws GeneralSecurityException If there's a security error
     */
    public Event removeAssigneeFromEventDescription(String eventId, String userEmail) 
            throws IOException, GeneralSecurityException {
        Calendar service = getServiceAccountCalendarService();
        String calendarId = getTeamCalendarIdWithServiceAccount();
        
        // Get the current event
        Event event = service.events().get(calendarId, eventId).execute();
        String description = event.getDescription();
        
        if (description != null && description.contains(userEmail)) {
            log.info("Removing user {} from event {} description", userEmail, eventId);
            
            // Simple replacement for email - this is a basic approach and might need refinement
            // based on the exact format of the description
            String pattern = "[^,]+\\(" + userEmail + "\\),?\\s*";
            description = description.replaceAll(pattern, "");
            
            // Clean up trailing commas
            description = description.replaceAll("Przypisany do: ,", "Przypisany do: ");
            description = description.replaceAll("\\)\\s*,\\s*$", ")");
            
            // If no assignees left, remove the line
            if (description.contains("Przypisany do: ") && 
                description.indexOf("Przypisany do: ") + "Przypisany do: ".length() >= description.length()) {
                description = description.replaceAll("\\n\\nPrzypisany do: $", "");
            }
            
            event.setDescription(description);
            
            // Update the event
            return service.events().update(calendarId, eventId, event).execute();
        }
        
        return event;
    }
    
    /**
     * Deletes an event from the calendar
     * 
     * @param adminUserId Admin user ID to access the calendar
     * @param eventId The event ID to delete
     * @throws IOException If there's an error deleting the event
     * @throws GeneralSecurityException If there's a security error
     */
    public void deleteEvent(UUID adminUserId, String eventId) throws IOException, GeneralSecurityException {
        Calendar service = getCalendarService(adminUserId);
        String calendarId = getTeamCalendarId(adminUserId);
        
        service.events().delete(calendarId, eventId).execute();
    }
    
    /**
     * Updates an existing event with new information
     * 
     * @param adminUserId Admin user ID to access the calendar
     * @param eventId The event ID to update
     * @param summary Updated summary/title
     * @param start Updated start time
     * @param end Updated end time
     * @return The updated event
     * @throws IOException If there's an error updating the event
     * @throws GeneralSecurityException If there's a security error
     */
    public Event updateEvent(UUID adminUserId, String eventId, String summary, 
                          ZonedDateTime start, ZonedDateTime end) 
                          throws IOException, GeneralSecurityException {
        Calendar service = getCalendarService(adminUserId);
        String calendarId = getTeamCalendarId(adminUserId);
        
        // Get the current event
        Event event = service.events().get(calendarId, eventId).execute();
        
        // Update the event details
        event.setSummary(summary);
        event.setStart(new EventDateTime()
            .setDateTime(new DateTime(start.toInstant().toEpochMilli()))
            .setTimeZone(TIMEZONE));
        event.setEnd(new EventDateTime()
            .setDateTime(new DateTime(end.toInstant().toEpochMilli()))
            .setTimeZone(TIMEZONE));
        
        // Update the event
        return service.events().update(calendarId, eventId, event).execute();
    }
    
    /**
     * For backward compatibility: Creates a new event in the user's primary Google Calendar.
     * 
     * @param userId UUID of the user for whom to create the event
     * @param eventDTO Event data containing details like title, location, and timing
     * @return The event ID
     */
    public String createEvent(UUID userId, EventDTO eventDTO) {
        try {
            log.info("Creating calendar event for user: {}", userId);
            
            // Convert date strings to ZonedDateTime objects
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm:ss");
            LocalDateTime startLocal = LocalDateTime.parse(eventDTO.startDateTime(), formatter);
            LocalDateTime endLocal = LocalDateTime.parse(eventDTO.endDateTime(), formatter);
            
            ZonedDateTime start = startLocal.atZone(ZoneId.of(eventDTO.timeZone()));
            ZonedDateTime end = endLocal.atZone(ZoneId.of(eventDTO.timeZone()));
            
            // Create event in team calendar
            Event event = createTaskEvent(userId, eventDTO.summary(), start, end, null);
            
            return event.getId();
        } catch (Exception e) {
            log.error("Error creating event: {}", e.getMessage());
            throw new RuntimeException("Unable to create event in calendar: " + e.getMessage());
        }
    }
    
    /**
     * For backward compatibility: Updates an existing event in the team calendar
     * 
     * @param userId Admin user ID to access the calendar
     * @param eventId The event ID to update
     * @param eventDTO Updated event data
     * @return The event ID
     */
    public String updateEvent(UUID userId, String eventId, EventDTO eventDTO) {
        try {
            log.info("Updating calendar event, eventId: {}", eventId);
            
            // Convert date strings to ZonedDateTime objects
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy:HH:mm:ss");
            LocalDateTime startLocal = LocalDateTime.parse(eventDTO.startDateTime(), formatter);
            LocalDateTime endLocal = LocalDateTime.parse(eventDTO.endDateTime(), formatter);
            
            ZonedDateTime start = startLocal.atZone(ZoneId.of(eventDTO.timeZone()));
            ZonedDateTime end = endLocal.atZone(ZoneId.of(eventDTO.timeZone()));
            
            // Update event in team calendar
            Event event = updateEvent(userId, eventId, eventDTO.summary(), start, end);
            
            return event.getId();
        } catch (Exception e) {
            log.error("Error updating event: {}", e.getMessage());
            throw new RuntimeException("Unable to update event: " + e.getMessage());
        }
    }

    /**
     * Updates an existing event in the team calendar using the service account
     * This preserves the event description with assignee information
     * 
     * @param eventId The event ID to update
     * @param summary Updated summary/title
     * @param start Updated start time
     * @param end Updated end time
     * @return The updated event
     * @throws IOException If there's an error updating the event
     * @throws GeneralSecurityException If there's a security error
     */
    public Event updateEventWithServiceAccount(String eventId, String summary, 
                          ZonedDateTime start, ZonedDateTime end) 
                          throws IOException, GeneralSecurityException {
        Calendar service = getServiceAccountCalendarService();
        String calendarId = getTeamCalendarIdWithServiceAccount();
        
        log.info("Updating calendar event with service account: {}", eventId);
        
        // Get the current event to preserve description with assignees
        Event event = service.events().get(calendarId, eventId).execute();
        
        // Update the event details but keep the description
        event.setSummary(summary);
        event.setStart(new EventDateTime()
            .setDateTime(new DateTime(start.toInstant().toEpochMilli()))
            .setTimeZone(TIMEZONE));
        event.setEnd(new EventDateTime()
            .setDateTime(new DateTime(end.toInstant().toEpochMilli()))
            .setTimeZone(TIMEZONE));
        
        // Update the event
        return service.events().update(calendarId, eventId, event).execute();
    }
    
    /**
     * Deletes an event from the calendar using the service account
     * 
     * @param eventId The event ID to delete
     * @throws IOException If there's an error deleting the event
     * @throws GeneralSecurityException If there's a security error
     */
    public void deleteEventWithServiceAccount(String eventId) throws IOException, GeneralSecurityException {
        Calendar service = getServiceAccountCalendarService();
        String calendarId = getTeamCalendarIdWithServiceAccount();
        
        log.info("Deleting calendar event with service account: {}", eventId);
        service.events().delete(calendarId, eventId).execute();
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

        ZonedDateTime adjustedDateTime = localDateTime.atZone(ZoneId.of(TIMEZONE));

        return adjustedDateTime.toString();
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
}

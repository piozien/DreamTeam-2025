package tech.project.schedule.dto.calendar;

/**
 * Data Transfer Object (DTO) for representing calendar events.
 * Used for transferring event information between the application and 
 * Google Calendar API when creating, updating, or accessing calendar events.
 * Implemented as a Java record for concise immutable data representation.
 */
public record EventDTO(String summary,
                       String location,
                       String description,
                       String startDateTime,
                       String endDateTime,
                       String timeZone)  {

}

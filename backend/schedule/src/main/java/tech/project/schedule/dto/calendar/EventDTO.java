package tech.project.schedule.dto.calendar;

/**
 * Data Transfer Object for calendar events used in Google Calendar integration.
 * Contains essential fields needed to create, update, or represent events in 
 * an external calendar system.
 */
public record EventDTO(String summary,
                       String location,
                       String description,
                       String startDateTime,
                       String endDateTime,
                       String timeZone)  {

}

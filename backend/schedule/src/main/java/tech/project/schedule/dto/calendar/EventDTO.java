package tech.project.schedule.dto.calendar;

public record EventDTO(String summary,
                       String location,
                       String description,
                       String startDateTime,
                       String endDateTime,
                       String timeZone)  {

}

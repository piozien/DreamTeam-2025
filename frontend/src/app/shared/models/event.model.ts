/**
 * Data Transfer Object for calendar events used in Google Calendar integration.
 * Matches the backend EventDTO structure.
 */
export interface EventDTO {
  summary: string;
  description: string;
  startDateTime: string;
  endDateTime: string;
  timeZone: string;
}

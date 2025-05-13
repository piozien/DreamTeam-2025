import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventDTO } from '../models/event.model';

@Injectable({
  providedIn: 'root'
})
export class GoogleCalendarService {
  private apiUrl = 'http://localhost:8080/api/calendar';

  constructor(private http: HttpClient) {}

  /**
   * Creates a new event in Google Calendar
   * @param eventDTO Event details to be added to calendar
   * @returns Observable with the created event ID
   */
  createEvent(eventDTO: EventDTO): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/create-event`, eventDTO);
  }

  /**
   * Updates an existing event in Google Calendar
   * @param eventId ID of the event to update
   * @param eventDTO Updated event details
   * @returns Observable with response from backend
   */
  updateEvent(eventId: string, eventDTO: EventDTO): Observable<string> {
    return this.http.put<string>(`${this.apiUrl}/update-event/${eventId}`, eventDTO);
  }

  /**
   * Deletes an event from Google Calendar
   * @param eventId ID of the event to delete
   * @returns Observable with void response
   */
  deleteEvent(eventId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/delete-event/${eventId}`);
  }

  /**
   * Converts a task date to the format required by Google Calendar
   * @param dateString Date in format YYYY-MM-DD
   * @param timeOfDay Either 'start' (00:00:00) or 'end' (23:59:59)
   * @returns Formatted date-time string
   */
  formatDateForCalendar(dateString: string, timeOfDay: 'start' | 'end'): string {
    if (!dateString) return '';
    
    // Parse the date string
    const [year, month, day] = dateString.split('-').map(part => parseInt(part, 10));
    
    // Add either start of day or end of day time based on parameter
    let timeString = '00:00:00';
    if (timeOfDay === 'end') {
      timeString = '23:59:59';
    }
    
    // Format in the dd.MM.yyyy:HH:mm:ss format expected by backend
    return `${day.toString().padStart(2, '0')}.${month.toString().padStart(2, '0')}.${year}:${timeString}`;
  }

  /**
   * Creates a Google Calendar event from a task
   * @param taskName Task name (becomes event summary)
   * @param taskDescription Task description
   * @param startDate Task start date (YYYY-MM-DD)
   * @param endDate Task end date (YYYY-MM-DD)
   * @returns Observable with the created event ID
   */
  createTaskEvent(taskName: string, taskDescription: string, startDate: string, endDate?: string): Observable<string> {
    // If no end date is provided, use start date as end date
    const eventEndDate = endDate || startDate;
    
    const eventDTO: EventDTO = {
      summary: taskName,
      description: taskDescription || 'No description provided',
      startDateTime: this.formatDateForCalendar(startDate, 'start'),
      endDateTime: this.formatDateForCalendar(eventEndDate, 'end'),
      timeZone: 'Europe/Warsaw' // Use Polish timezone as in backend
    };
    
    return this.createEvent(eventDTO);
  }
  
  /**
   * Tests the Google Calendar token to verify it's valid and working
   * @returns Observable with the test response message
   */
  testToken(): Observable<any> {
    return this.http.get(`${this.apiUrl}/test-token`);
  }
}

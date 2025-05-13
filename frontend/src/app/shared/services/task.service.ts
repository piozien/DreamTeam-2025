import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, tap } from 'rxjs';
import { GoogleCalendarService } from './google-calendar.service';
import { Task, TaskRequest, TaskUpdate, TaskComment, TaskFile, TaskAssignee } from '../models/task.model';

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private apiUrl = 'http://localhost:8080/api/tasks';

  constructor(
    private http: HttpClient,
    private googleCalendarService: GoogleCalendarService
  ) {}

  // Get all tasks for a project
  getTasksByProject(projectId: string, userId: string): Observable<Task[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<Task[]>(`${this.apiUrl}/project/${projectId}`, { params });
  }

  // Get a single task by ID
  getTaskById(taskId: string, userId: string): Observable<Task> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<Task>(`${this.apiUrl}/${taskId}`, { params });
  }

  // Create a new task
  createTask(task: TaskRequest, userId: string): Observable<Task> {
    const params = new HttpParams().set('userId', userId);
    
    // Make sure we have proper UUID format for projectId
    const formattedTask = {
      ...task,
      // Convert empty arrays to empty collections for Java
      assigneeIds: task.assigneeIds || [],
      comments: task.comments || [],
      files: task.files || [],
      dependencyIds: task.dependencyIds || []
    };
    
    console.log('Formatted task being sent to API:', formattedTask);
    return this.http.post<Task>(this.apiUrl, formattedTask, { params })
      .pipe(
        tap(createdTask => {
          // After successfully creating the task, add it to Google Calendar
          console.log('Task created successfully, adding to Google Calendar');
          
          this.addTaskToGoogleCalendar(createdTask);
        }),
        catchError(error => {
          console.error('Error creating task:', error);
          throw error;
        })
      );
  }

  // Update a task
  updateTask(taskId: string, task: TaskUpdate, userId: string): Observable<Task> {
    const params = new HttpParams().set('userId', userId);
    return this.http.put<Task>(`${this.apiUrl}/${taskId}`, task, { params });
  }

  // Delete a task
  deleteTask(taskId: string, userId: string): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.delete<void>(`${this.apiUrl}/${taskId}`, { params });
  }

  // Task Assignees
  getTaskAssignees(taskId: string, userId: string): Observable<TaskAssignee[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<TaskAssignee[]>(`${this.apiUrl}/${taskId}/assignees`, { params });
  }

  addAssignee(taskId: string, assigneeUserId: string, currentUserId: string): Observable<TaskAssignee> {
    // Create query parameter for currentUserId
    const params = new HttpParams().set('currentUserId', currentUserId);
    
    // Create a properly formatted TaskAssigneeDTO as expected by the backend
    // Ensure we're strictly matching the expected structure
    const assigneeData = {
      userId: assigneeUserId
    };
    
    console.log('Adding assignee with payload:', JSON.stringify(assigneeData), 'currentUserId:', currentUserId);
    
    // Log the full request URL for debugging
    const url = `${this.apiUrl}/${taskId}/assignees`;
    console.log('Request URL:', url);
    
    return this.http.post<TaskAssignee>(
      url,
      assigneeData,
      { 
        params,
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        // Add response type to ensure we only get the JSON data we need
        responseType: 'json' as const
      }
    );
  }

  removeAssignee(taskId: string, assigneeId: string, currentUserId: string): Observable<void> {
    const params = new HttpParams().set('currentUserId', currentUserId);
    return this.http.delete<void>(`${this.apiUrl}/${taskId}/assignees/${assigneeId}`, { params });
  }

  // Task Comments
  getTaskComments(taskId: string, userId: string): Observable<TaskComment[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<TaskComment[]>(`${this.apiUrl}/${taskId}/comments`, { params });
  }

  addComment(taskId: string, comment: string, userId: string): Observable<TaskComment> {
    const params = new HttpParams().set('userId', userId);
    return this.http.post<TaskComment>(`${this.apiUrl}/${taskId}/comments`, { comment, taskId, userId }, { params });
  }

  deleteComment(taskId: string, commentId: string, userId: string): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.delete<void>(`${this.apiUrl}/${taskId}/comments/${commentId}`, { params });
  }

  // Task Files
  getTaskFiles(taskId: string, userId: string): Observable<TaskFile[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<TaskFile[]>(`${this.apiUrl}/${taskId}/files`, { params });
  }

  addFile(taskId: string, file: TaskFile, userId: string): Observable<TaskFile> {
    const params = new HttpParams().set('userId', userId);
    return this.http.post<TaskFile>(`${this.apiUrl}/${taskId}/files`, file, { params });
  }

  deleteFile(taskId: string, fileId: string, userId: string): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.delete<void>(`${this.apiUrl}/${taskId}/files/${fileId}`, { params });
  }

  // Task Dependencies
  getTaskDependencies(taskId: string, userId: string): Observable<Task[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<Task[]>(`${this.apiUrl}/dependencies/${taskId}/dependencies`, { params });
  }

  addDependency(taskId: string, dependencyId: string, userId: string): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.post<void>(`${this.apiUrl}/${taskId}/dependencies/${dependencyId}`, null, { params });
  }

  removeDependency(taskId: string, dependencyId: string, userId: string): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.delete<void>(`${this.apiUrl}/${taskId}/dependencies/${dependencyId}`, { params });
  }

  updateDependency(taskId: string, dependencyId: string, userId: string): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.put<void>(`${this.apiUrl}/${taskId}/dependencies/${dependencyId}`, null, { params });
  }

  // Get all tasks assigned to a user
  getUserTasks(userId: string): Observable<Task[]> {
    const params = new HttpParams().set('requestUserId', userId);
    return this.http.get<Task[]>(`${this.apiUrl}/user/${userId}`, { params });
  }

  /**
   * Adds a task to Google Calendar
   * @param task The task to add to Google Calendar
   * @private
   */
  private addTaskToGoogleCalendar(task: Task): void {
    if (!task.startDate) {
      console.warn('Cannot add task to Google Calendar without a start date');
      return;
    }

    this.googleCalendarService.createTaskEvent(
      task.name,
      task.description || '',
      task.startDate,
      task.endDate
    ).subscribe({
      next: (eventId) => {
        console.log(`Task successfully added to Google Calendar with event ID: ${eventId}`);
        // Optionally, we could store the eventId with the task for future reference
        // This would require updating the Task model and backend
      },
      error: (error) => {
        console.error('Error adding task to Google Calendar:', error);
      }
    });
  }
}

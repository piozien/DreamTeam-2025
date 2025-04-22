import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Task, TaskRequest, TaskUpdate, TaskComment, TaskFile, TaskAssignee } from '../models/task.model';

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private apiUrl = 'http://localhost:8080/api/tasks';

  constructor(private http: HttpClient) {}

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
    return this.http.post<Task>(this.apiUrl, formattedTask, { params });
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
    const params = new HttpParams().set('currentUserId', currentUserId);
    return this.http.post<TaskAssignee>(`${this.apiUrl}/${taskId}/assignees`, { userId: assigneeUserId }, { params });
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
  addDependency(taskId: string, dependencyId: string, userId: string): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.post<void>(`${this.apiUrl}/${taskId}/dependencies/${dependencyId}`, {}, { params });
  }

  removeDependency(taskId: string, dependencyId: string, userId: string): Observable<void> {
    const params = new HttpParams().set('userId', userId);
    return this.http.delete<void>(`${this.apiUrl}/${taskId}/dependencies/${dependencyId}`, { params });
  }
}

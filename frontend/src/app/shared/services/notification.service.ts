import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Notification } from '../models/notification.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);
  
  private apiUrl = 'http://localhost:8080/api/notifications';
  
  // Observable sources
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  
  // Observable streams
  public notifications$ = this.notificationsSubject.asObservable();
  public unreadCount$ = this.unreadCountSubject.asObservable();

  /**
   * Fetches all notifications for the currently authenticated user
   */
  getNotifications(): Observable<Notification[]> {
    const userId = this.authService.getUserId();
    
    if (!userId) {
      throw new Error('User not authenticated');
    }
    
    return this.http.get<Notification[]>(`${this.apiUrl}?userId=${userId}&authenticatedUserId=${userId}`).pipe(
      tap(notifications => {
        this.notificationsSubject.next(notifications);
        this.updateUnreadCount(notifications);
      })
    );
  }

  /**
   * Marks a specific notification as read
   */
  markAsRead(notificationId: string): Observable<Notification> {
    const userId = this.authService.getUserId();
    
    if (!userId) {
      throw new Error('User not authenticated');
    }
    
    return this.http.put<Notification>(`${this.apiUrl}/${notificationId}?userId=${userId}`, {}).pipe(
      tap(updatedNotification => {
        // Update the notifications list with the modified notification
        const currentNotifications = this.notificationsSubject.value;
        const updatedNotifications = currentNotifications.map(notification => 
          notification.id === updatedNotification.id ? updatedNotification : notification
        );
        
        this.notificationsSubject.next(updatedNotifications);
        this.updateUnreadCount(updatedNotifications);
      })
    );
  }

  /**
   * Marks all notifications as read for the current user
   */
  markAllAsRead(): Observable<Notification[]> {
    const userId = this.authService.getUserId();
    
    if (!userId) {
      throw new Error('User not authenticated');
    }
    
    return this.http.put<Notification[]>(`${this.apiUrl}/read-all?userId=${userId}`, {}).pipe(
      tap(updatedNotifications => {
        this.notificationsSubject.next(updatedNotifications);
        this.updateUnreadCount(updatedNotifications);
      })
    );
  }

  /**
   * Deletes a specific notification
   */
  deleteNotification(notificationId: string): Observable<string> {
    const userId = this.authService.getUserId();
    
    if (!userId) {
      throw new Error('User not authenticated');
    }
    
    return this.http.delete<string>(`${this.apiUrl}/${notificationId}?userId=${userId}`).pipe(
      tap(() => {
        // Remove the deleted notification from the list
        const currentNotifications = this.notificationsSubject.value;
        const updatedNotifications = currentNotifications.filter(
          notification => notification.id !== notificationId
        );
        
        this.notificationsSubject.next(updatedNotifications);
        this.updateUnreadCount(updatedNotifications);
      })
    );
  }

  /**
   * Deletes all notifications for the current user
   */
  deleteAllNotifications(): Observable<string> {
    const userId = this.authService.getUserId();
    
    if (!userId) {
      throw new Error('User not authenticated');
    }
    
    return this.http.delete<string>(`${this.apiUrl}/all?userId=${userId}`).pipe(
      tap(() => {
        // Clear all notifications
        this.notificationsSubject.next([]);
        this.unreadCountSubject.next(0);
      })
    );
  }

  /**
   * Updates the unread notifications count
   */
  private updateUnreadCount(notifications: Notification[]): void {
    const unreadCount = notifications.filter(notification => !notification.isRead).length;
    this.unreadCountSubject.next(unreadCount);
  }
}

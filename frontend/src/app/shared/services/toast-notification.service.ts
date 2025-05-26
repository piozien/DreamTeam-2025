import { Injectable, inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { WebSocketService } from './websocket.service';
import { Subject } from 'rxjs';

export interface ToastNotification {
  type: 'success' | 'error' | 'info' | 'warning';
  message: string;
  title?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ToastNotificationService {
  private toastr = inject(ToastrService);
  private webSocketService = inject(WebSocketService);
  
  // Subject for direct toast notifications (non-websocket)
  private toastSubject = new Subject<ToastNotification>();
  public toast$ = this.toastSubject.asObservable();
  
  // Map WebSocket notification status to toast type
  private notificationTypeMap: Record<string, 'success' | 'error' | 'info' | 'warning'> = {
    // Project notifications
    'PROJECT_CREATED': 'success',
    'PROJECT_UPDATED': 'info',
    'PROJECT_MEMBER_ADDED': 'info',
    'PROJECT_MEMBER_REMOVED': 'info',
    'PROJECT_DELETED': 'warning',
    
    // Task notifications
    'TASK_ASSIGNEE_ADDED': 'info',
    'TASK_UPDATED': 'info',
    'TASK_COMPLETED': 'success',
    'TASK_DELETED': 'warning',
    'TASK_COMMENT_ADDED': 'info',
    'TASK_COMMENT_DELETED': 'info',
    'TASK_DEPENDENCY_ADDED': 'info',
    'TASK_DEPENDENCY_DELETED': 'info',
    'TASK_FILE_UPLOADED': 'info',
    'TASK_FILE_UPDATED': 'info',
    'TASK_FILE_DELETED': 'info',
    
    // Error and system notifications
    'ERROR': 'error',
    'PERMISSION_DENIED': 'error',
    'SYSTEM': 'info',
    
    // Additional notification types
    'DEADLINE_APPROACHING': 'warning',
    'TASK_OVERDUE': 'warning',
    'MENTION': 'info'
  };
  
  // Default messages for each notification type
  private defaultMessages: Record<string, string> = {
    'PROJECT_CREATED': 'Projekt został utworzony',
    'PROJECT_UPDATED': 'Projekt został zaktualizowany',
    'PROJECT_MEMBER_ADDED': 'Dodano użytkownika do projektu',
    'PROJECT_MEMBER_REMOVED': 'Usunięto użytkownika z projektu',
    'PROJECT_DELETED': 'Projekt został usunięty',
    'TASK_ASSIGNEE_ADDED': 'Zostałeś dodany do zadania',
    'TASK_UPDATED': 'Zadanie zostało zaktualizowane',
    'TASK_COMPLETED': 'Zadanie zostało oznaczone jako zakończone',
    'TASK_DELETED': 'Zadanie zostało usunięte',
    'TASK_COMMENT_ADDED': 'Dodano nowy komentarz do zadania',
    'TASK_COMMENT_DELETED': 'Usunięto komentarz z zadania',
    'TASK_DEPENDENCY_ADDED': 'Dodano nową zależność do zadania',
    'TASK_DEPENDENCY_DELETED': 'Usunięto zależność z zadania',
    'TASK_FILE_UPLOADED': 'Dodano nowy plik do zadania',
    'TASK_FILE_UPDATED': 'Zaktualizowano plik w zadaniu',
    'TASK_FILE_DELETED': 'Usunięto plik z zadania',
    'ERROR': 'Wystąpił błąd',
    'PERMISSION_DENIED': 'Brak uprawnień',
    'SYSTEM': 'Komunikat systemowy',
    'DEADLINE_APPROACHING': 'Zbliża się termin zadania',
    'TASK_OVERDUE': 'Zadanie jest przeterminowane',
    'MENTION': 'Zostałeś wspomniany w komentarzu'
  };

  constructor() {
    // Subscribe to WebSocket notifications
    this.webSocketService.notifications$.subscribe({
      next: (notification) => {
        const toast = this.processWebSocketNotification(notification);
        if (toast) {
          this.showToast(toast);
        }
      },
      error: (error) => {
        console.error('Error in WebSocket notification stream:', error);
      }
    });
  }
  
  /**
   * Show a toast notification
   */
  private showToast(notification: ToastNotification): void {
    const { type, message, title } = notification;
    switch (type) {
      case 'success':
        this.toastr.success(message, title);
        break;
      case 'error':
        this.toastr.error(message, title);
        break;
      case 'warning':
        this.toastr.warning(message, title);
        break;
      case 'info':
      default:
        this.toastr.info(message, title);
        break;
    }
  }
  
  /**
   * Show a success toast notification
   */
  success(message: string, title?: string): void {
    this.toastSubject.next({ type: 'success', message, title });
  }
  
  /**
   * Show an error toast notification
   */
  error(message: string, title?: string): void {
    this.toastSubject.next({ type: 'error', message, title });
  }
  
  /**
   * Show an info toast notification
   */
  info(message: string, title?: string): void {
    this.toastSubject.next({ type: 'info', message, title });
  }
  
  /**
   * Show a warning toast notification
   */
  warning(message: string, title?: string): void {
    this.toastSubject.next({ type: 'warning', message, title });
  }
  
  /**
   * Clear all toasts
   */
  clear(): void {
    this.toastr.clear();
  }
  
  /**
   * Process a WebSocket notification and determine its toast type
   */
  processWebSocketNotification(notification: any): ToastNotification | null {
    if (!notification || !notification.status) {
      return null;
    }
    
    // Convert status to uppercase to match our mapping
    const statusKey = notification.status.toUpperCase();
    const type = this.notificationTypeMap[statusKey] || 'info';
    
    // Try to get the message from the notification, then from default messages with uppercase key,
    // then fall back to a generic message
    const message = notification.message || 
                   this.defaultMessages[statusKey] || 
                   this.defaultMessages[notification.status] ||
                   `Nowe powiadomienie: ${notification.status}`;
    
    console.log('Processing notification:', { originalStatus: notification.status, statusKey, type, message });
    
    return { type, message };
  }
}

import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription } from 'rxjs';
import { Notification } from '../../shared/models/notification.model';
import { NotificationService } from '../../shared/services/notification.service';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-notification-center',
  templateUrl: './notification-center.component.html',
  styleUrls: ['./notification-center.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatBadgeModule,
    MatMenuModule,
    MatButtonModule,
    MatDividerModule,
    MatTooltipModule,
    DatePipe
  ]
})
export class NotificationCenterComponent implements OnInit, OnDestroy {
  private notificationService = inject(NotificationService);
  private subscriptions: Subscription[] = [];
  
  notifications: Notification[] = [];
  unreadCount: number = 0;
  isLoading: boolean = false;

  ngOnInit(): void {
    // Subscribe to notifications
    this.subscriptions.push(
      this.notificationService.notifications$.subscribe(
        notifications => this.notifications = notifications
      )
    );
    
    // Subscribe to unread count
    this.subscriptions.push(
      this.notificationService.unreadCount$.subscribe(
        count => this.unreadCount = count
      )
    );
    
    // Load notifications initially
    this.loadNotifications();
  }
  
  ngOnDestroy(): void {
    // Clean up subscriptions
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }
  
  /**
   * Loads notifications from the backend
   */
  loadNotifications(): void {
    this.isLoading = true;
    this.notificationService.getNotifications().subscribe({
      next: () => {
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading notifications:', error);
        this.isLoading = false;
      }
    });
  }
  
  /**
   * Marks a single notification as read
   */
  markAsRead(notification: Notification, event: Event): void {
    event.stopPropagation(); // Prevent menu from closing
    
    if (notification.isRead) {
      return; // Skip if already read
    }
    
    this.notificationService.markAsRead(notification.id).subscribe({
      error: (error) => console.error('Error marking notification as read:', error)
    });
  }
  
  /**
   * Marks all notifications as read
   */
  markAllAsRead(event: Event): void {
    event.stopPropagation(); // Prevent menu from closing
    
    this.notificationService.markAllAsRead().subscribe({
      error: (error) => console.error('Error marking all notifications as read:', error)
    });
  }
  
  /**
   * Deletes a single notification
   */
  deleteNotification(notification: Notification, event: Event): void {
    event.stopPropagation(); // Prevent menu from closing
    
    this.notificationService.deleteNotification(notification.id).subscribe({
      error: (error) => console.error('Error deleting notification:', error)
    });
  }
  
  /**
   * Deletes all notifications
   */
  deleteAllNotifications(event: Event): void {
    event.stopPropagation(); // Prevent menu from closing
    
    this.notificationService.deleteAllNotifications().subscribe({
      error: (error) => console.error('Error deleting all notifications:', error)
    });
  }
  
  /**
   * Refreshes the notifications list
   */
  refreshNotifications(event: Event): void {
    event.stopPropagation(); // Prevent menu from closing
    this.loadNotifications();
  }
}

import { Component, OnInit, Inject, PLATFORM_ID, OnDestroy } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { inject } from '@angular/core';
import { AuthService } from '../../shared/services/auth.service';
import { User } from '../../shared/models/user.model';
import { WebSocketService } from '../../shared/services/websocket.service';
import { NotificationService } from '../../shared/services/notification.service';
import { NotificationCenterComponent } from '../notification-center/notification-center.component';
import { Subscription } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { ToastNotificationService } from '../../shared/services/toast-notification.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatToolbarModule,
    MatButtonModule,
    MatMenuModule,
    NotificationCenterComponent
  ]
})
export class NavbarComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private webSocketService = inject(WebSocketService);
  private notificationService = inject(NotificationService);
  private toastService = inject(ToastrService);
  private toastNotificationService = inject(ToastNotificationService);
  private router = inject(Router);
  private isBrowser: boolean;
  private subscriptions: Subscription[] = [];
  
  currentUser: User | null = null;
  isLoggedIn = false;
  isAdmin = false;
  isSocketConnected = false;

  constructor(@Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    // Subscribe to the authentication state changes
    this.subscriptions.push(
      this.authService.currentUser$.subscribe((user: User | null) => {
        this.currentUser = user;
        this.isLoggedIn = !!user;
        
        // Check if the current user has admin privileges
        this.isAdmin = this.authService.isAdmin();

        // Handle WebSocket connection only in browser
        if (this.isBrowser) {
          if (user) {
            // Only initialize WebSocket if user is logged in
            this.webSocketService.connect();
          } else {
            // Disconnect WebSocket when user logs out
            this.webSocketService.disconnect();
          }
        }
      })
    );
    
    // Subscribe to WebSocket connection status
    this.subscriptions.push(
      this.webSocketService.connected$.subscribe(connected => {
        this.isSocketConnected = connected;
      })
    );
    
    // Subscribe to WebSocket notifications
    this.subscriptions.push(
      this.webSocketService.notifications$.subscribe(notification => {
        console.log('Received notification in navbar:', notification);
        
        // Refresh notifications list
        this.notificationService.getNotifications().subscribe();
        
        // Show toast notification
        const toastNotification = this.toastNotificationService.processWebSocketNotification(notification);
        if (toastNotification) {
          switch (toastNotification.type) {
            case 'success':
              this.toastService.success(toastNotification.message, toastNotification.title);
              break;
            case 'error':
              this.toastService.error(toastNotification.message, toastNotification.title);
              break;
            case 'info':
              this.toastService.info(toastNotification.message, toastNotification.title);
              break;
            case 'warning':
              this.toastService.warning(toastNotification.message, toastNotification.title);
              break;
          }
        }
      })
    );
    
    // Subscribe to direct toast notifications
    this.subscriptions.push(
      this.toastNotificationService.toast$.subscribe(notification => {
        if (notification) {
          switch (notification.type) {
            case 'success':
              this.toastService.success(notification.message, notification.title);
              break;
            case 'error':
              this.toastService.error(notification.message, notification.title);
              break;
            case 'info':
              this.toastService.info(notification.message, notification.title);
              break;
            case 'warning':
              this.toastService.warning(notification.message, notification.title);
              break;
          }
        }
      })
    );
  }
  
  ngOnDestroy(): void {
    // Clean up subscriptions when component is destroyed
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  logout(): void {
    this.webSocketService.disconnect();
    this.authService.logout();
    this.router.navigate(['/auth/login']);
    this.toastService.clear();
  }
}
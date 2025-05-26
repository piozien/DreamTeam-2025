import { Injectable, inject, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AuthService } from './auth.service';
import { BehaviorSubject, Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private readonly authService = inject(AuthService);
  private readonly isBrowser: boolean;

  private SockJS: any = null;
  private Stomp: any = null;
  private socketClient: any = null;
  private notificationSubscription: any;
  
  // Connection status subject
  private connectedSubject = new BehaviorSubject<boolean>(false);
  public connected$ = this.connectedSubject.asObservable();

  // Notification subject
  private notificationSubject = new Subject<any>();
  public notifications$ = this.notificationSubject.asObservable();

  constructor(@Inject(PLATFORM_ID) platformId: Object) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  /**
   * Initialize and connect to WebSocket if user is logged in
   */
  public async connect(): Promise<void> {
    if (!this.isBrowser) return;
    
    try {
      await this.setupWebSocket();
    } catch (error) {
      console.error('Error connecting to WebSocket:', error);
    }
  }

  /**
   * Disconnect from WebSocket
   */
  public disconnect(): void {
    this.disconnectWebSocket();
  }

  /**
   * Check if WebSocket is connected
   */
  public isConnected(): boolean {
    return !!this.socketClient && this.socketClient.connected;
  }

  /**
   * Setup WebSocket connection
   */
  private async setupWebSocket(): Promise<void> {
    try {
      console.log('Setting up WebSocket connection...');
      // Only load libraries if they haven't been loaded yet
      if (!this.SockJS || !this.Stomp) {
        console.log('Loading WebSocket libraries...');
        // Dynamically import the required libraries
        const sockjsModule = await import('sockjs-client');
        const stompModule = await import('stompjs');
        
        this.SockJS = sockjsModule.default;
        this.Stomp = stompModule;
        console.log('WebSocket libraries loaded');
      }
      
      // Now initialize the connection
      console.log('Initializing WebSocket connection...');
      this.initWebSocket();
    } catch (error) {
      console.error('Error setting up WebSocket:', error);
      this.connectedSubject.next(false);
    }
  }

  /**
   * Initialize WebSocket connection
   */
  private initWebSocket(): void {
    // Safety check
    if (!this.SockJS || !this.Stomp || !this.isBrowser) {
      console.error('WebSocket initialization failed: Missing dependencies or not in browser');
      return;
    }
    
    // Disconnect existing connection if any
    this.disconnectWebSocket();
    
    try {
      const token = this.authService.getToken();
      const userId = this.authService.getUserId();
      
      if (!token || !userId) {
        console.error('Cannot connect to WebSocket: Missing token or user ID');
        return;
      }
      
      console.log('Creating new WebSocket connection...');
      const ws = new this.SockJS('http://localhost:8080/ws');
      this.socketClient = this.Stomp.over(ws);
      
      // Prevent debug logs from Stomp
      this.socketClient.debug = (message: string) => {
        // Only log WebSocket messages at debug level
        console.debug('[WebSocket]', message);
      };
      
      console.log('Connecting to WebSocket server...');
      // Connect with headers
      this.socketClient.connect(
        { 
          'Authorization': `Bearer ${token}`,
          'X-UserId': userId // Add user ID to headers for debugging
        }, 
        () => {
          console.log('Successfully connected to WebSocket server');
          this.connectedSubject.next(true);
          
          // Subscribe to notifications - use the correct destination format
          const destination = `/user/${userId}/queue/notifications`;
          console.log(`Subscribing to notifications at: ${destination}`);
          
          this.notificationSubscription = this.socketClient.subscribe(
            destination,
            (message: any) => {
              console.log('Received raw WebSocket message:', message);
              try {
                const notification = JSON.parse(message.body);
                console.log('Successfully parsed notification:', notification);
                // Emit notification through subject
                this.notificationSubject.next(notification);
              } catch (e) {
                console.error('Error parsing notification:', e, 'Raw message:', message);
              }
            },
            { 'durable': false, 'auto-delete': true, 'exclusive': false }
          );
          
          console.log('Successfully subscribed to notifications');
        }, 
        (error: any) => {
          console.error('Error connecting to WebSocket server:', error);
          this.socketClient = null;
          this.connectedSubject.next(false);
          
          // Try to reconnect after a delay
          console.log('Will attempt to reconnect in 5 seconds...');
          setTimeout(() => this.setupWebSocket(), 5000);
        }
      );
      
      // Handle WebSocket errors
      ws.onerror = (error: Event) => {
        console.error('WebSocket error:', error);
        this.connectedSubject.next(false);
      };
      
      ws.onclose = (event: CloseEvent) => {
        console.log('WebSocket connection closed:', event);
        this.connectedSubject.next(false);
        
        // Try to reconnect if this wasn't an intentional disconnect
        if (this.socketClient) {
          console.log('WebSocket connection lost. Attempting to reconnect...');
          setTimeout(() => this.setupWebSocket(), 5000);
        }
      };
    } catch (error) {
      console.error('Error initializing WebSocket:', error);
      this.socketClient = null;
      this.connectedSubject.next(false);
    }
  }

  /**
   * Disconnect WebSocket connection
   */
  private disconnectWebSocket(): void {
    if (this.isBrowser) {
      if (this.notificationSubscription) {
        try {
          this.notificationSubscription.unsubscribe();
        } catch (error) {
          console.error('Error unsubscribing from WebSocket:', error);
        }
        this.notificationSubscription = null;
      }
      
      if (this.socketClient && this.socketClient.connected) {
        try {
          this.socketClient.disconnect();
          console.log('Disconnected from WebSocket');
        } catch (error) {
          console.error('Error disconnecting from WebSocket:', error);
        }
      }
      
      this.socketClient = null;
      this.connectedSubject.next(false);
    }
  }
}

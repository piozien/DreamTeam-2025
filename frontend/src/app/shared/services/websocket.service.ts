import { Injectable, inject, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { AuthService } from './auth.service';
import { BehaviorSubject, Observable } from 'rxjs';

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
      // Only load libraries if they haven't been loaded yet
      if (!this.SockJS || !this.Stomp) {
        // Dynamically import the required libraries
        const sockjsModule = await import('sockjs-client');
        const stompModule = await import('stompjs');
        
        this.SockJS = sockjsModule.default;
        this.Stomp = stompModule;
      }
      
      // Now initialize the connection
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
    if (!this.SockJS || !this.Stomp || !this.isBrowser) return;
    
    // Disconnect existing connection if any
    this.disconnectWebSocket();
    
    try {
      const token = this.authService.getToken();
      const ws = new this.SockJS('http://localhost:8080/ws');
      this.socketClient = this.Stomp.over(ws);
      
      // Prevent debug logs from Stomp
      this.socketClient.debug = null;
      
      this.socketClient.connect({'Authorization': `Bearer ${token}` }, 
        () => {
          console.log('Connected to WebSocket');
          this.connectedSubject.next(true);
          // You can add default subscriptions here if needed
          this.notificationSubscription = this.socketClient.subscribe(
            `/user/${token}/notifications`,
            (message: any) => {
              console.log('Received notification:', message.body);
            }
          );
        }, 
        (error: any) => {
          console.error('Error connecting to WebSocket:', error);
          this.socketClient = null;
          this.connectedSubject.next(false);
        }
      );
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
    if (this.isBrowser && this.socketClient) {
      try {
        this.socketClient.disconnect();
        console.log('Disconnected from WebSocket');
      } catch (error) {
        console.error('Error disconnecting from WebSocket:', error);
      } finally {
        this.socketClient = null;
        this.connectedSubject.next(false);
      }
    }
  }
}
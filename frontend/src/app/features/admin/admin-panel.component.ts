import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';


import { UserManagementComponent } from './components/user-management/user-management.component';
import { RegisterUserComponent } from './components/register-user/register-user.component';
import { AuthService } from '../../shared/services/auth.service';
import { GoogleCalendarService } from '../../shared/services/google-calendar.service';

@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTabsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,

    UserManagementComponent,
    RegisterUserComponent
  ],
  template: `
    <div class="container mt-4">
      <mat-card>
        <mat-card-header>
          <mat-card-title>
            <h1>Admin Panel</h1>
          </mat-card-title>
        </mat-card-header>
        
        <mat-card-content>
          <mat-tab-group animationDuration="0ms">
            <mat-tab label="User Management">
              <app-user-management></app-user-management>
            </mat-tab>
            <mat-tab label="Register New User">
              <app-register-user></app-register-user>
            </mat-tab>
            <mat-tab label="System Tools">
              <div class="system-tools-container">
                <h2>Google Calendar Integration</h2>
                <div class="tool-section">
                  <p>Verify that the Google Calendar integration is working correctly.</p>
                  <button mat-raised-button color="primary" (click)="testGoogleCalendarToken()" [disabled]="isTestingToken">
                    <mat-icon>verified</mat-icon>
                    {{ isTestingToken ? 'Testing...' : 'Test Calendar Token' }}
                  </button>
                </div>
              </div>
            </mat-tab>
          </mat-tab-group>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .container {
      padding: 20px;
      max-width: 1200px;
      margin: 0 auto;
    }
    
    mat-card {
      margin-bottom: 20px;
    }
    
    mat-card-title {
      margin-bottom: 20px;
    }
    
    mat-tab-group {
      margin-top: 20px;
    }

    .system-tools-container {
      padding: 20px;
    }

    .tool-section {
      margin-bottom: 30px;
      background-color: #f9f9f9;
      padding: 20px;
      border-radius: 8px;
      border-left: 4px solid #3f51b5;
    }

    .tool-section h3 {
      margin-top: 0;
      color: #3f51b5;
    }

    .tool-section p {
      margin-bottom: 15px;
    }
  `]
})
export class AdminPanelComponent implements OnInit {
  isTestingToken = false;
  
  constructor(
    private authService: AuthService,
    private router: Router,
    private googleCalendarService: GoogleCalendarService,
    private snackBar: MatSnackBar
  ) {}
  
  ngOnInit(): void {
    // Verify the user is an admin, if not redirect to home
    if (!this.authService.isAdmin()) {
      this.router.navigate(['/projects']);
    }
  }
  
  /**
   * Tests the Google Calendar token to verify it's valid
   */
  testGoogleCalendarToken(): void {
    this.isTestingToken = true;
    
    this.googleCalendarService.testToken().subscribe({
      next: (response) => {
        console.log('Calendar token test response:', response);
        this.snackBar.open('Google Calendar token is valid and working! 🎉', 'Close', {
          duration: 5000,
          panelClass: 'success-snackbar'
        });
        this.isTestingToken = false;
      },
      error: (error) => {
        console.error('Calendar token test error:', error);
        this.snackBar.open('Google Calendar token is invalid or expired. Please check your configuration.', 'Close', {
          duration: 10000,
          panelClass: 'error-snackbar'
        });
        this.isTestingToken = false;
      }
    });
  }
}

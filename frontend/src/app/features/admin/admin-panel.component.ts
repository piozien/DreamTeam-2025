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
  templateUrl: './admin-panel.component.html',
  styleUrls: ['./admin-panel.component.scss']
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

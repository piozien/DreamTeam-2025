import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';


import { UserManagementComponent } from './components/user-management/user-management.component';
import { RegisterUserComponent } from './components/register-user/register-user.component';
import { AuthService } from '../../shared/services/auth.service';

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

  `]
})
export class AdminPanelComponent implements OnInit {

  
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}
  
  ngOnInit(): void {
    // Verify the user is an admin, if not redirect to home
    if (!this.authService.isAdmin()) {
      this.router.navigate(['/projects']);
    }
  }
}

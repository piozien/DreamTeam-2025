import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule } from '@angular/material/dialog';
import { HttpClient } from '@angular/common/http';

import { AdminService } from '../../../../shared/services/admin.service';
import { AuthService } from '../../../../shared/services/auth.service';
import { User } from '../../../../shared/models/user.model';
import { GlobalRole } from '../../../../shared/enums/global-role.enum';
import { UserStatus } from '../../../../shared/enums/user-status.enum';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatChipsModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  template: `
    <div class="user-management-container">
      <h2>User Management</h2>
      
      <div *ngIf="loading" class="loading-spinner">
        <mat-spinner diameter="50"></mat-spinner>
      </div>
      
      <div *ngIf="error" class="error-message">
        <p>{{ error }}</p>
        <button mat-raised-button color="primary" (click)="loadUsers()">
          Retry
        </button>
      </div>
      
      <div *ngIf="!loading && !error && users.length === 0" class="no-users">
        <p>No users found in the system.</p>
      </div>
      
      <div *ngIf="!loading && !error && users.length > 0" class="users-table-container">
        <table mat-table [dataSource]="users" class="mat-elevation-z8">
          <!-- User ID Column -->
          <ng-container matColumnDef="id">
            <th mat-header-cell *matHeaderCellDef>ID</th>
            <td mat-cell *matCellDef="let user">{{ user.id | slice:0:8 }}...</td>
          </ng-container>

          <!-- Name Column -->
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>Name</th>
            <td mat-cell *matCellDef="let user">{{ user.firstName }} {{ user.lastName }}</td>
          </ng-container>

          <!-- Username Column -->
          <ng-container matColumnDef="username">
            <th mat-header-cell *matHeaderCellDef>Username</th>
            <td mat-cell *matCellDef="let user">{{ user.username }}</td>
          </ng-container>

          <!-- Email Column -->
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Email</th>
            <td mat-cell *matCellDef="let user">{{ user.email }}</td>
          </ng-container>

          <!-- Role Column -->
          <ng-container matColumnDef="role">
            <th mat-header-cell *matHeaderCellDef>Role</th>
            <td mat-cell *matCellDef="let user">
              <span [class]="user.globalRole === GlobalRole.ADMIN ? 'admin-role' : 'user-role'">
                {{ user.globalRole }}
              </span>
            </td>
          </ng-container>

          <!-- Status Column -->
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let user">
              <span [class]="'status-' + user.userStatus.toLowerCase()">
                {{ user.userStatus }}
              </span>
            </td>
          </ng-container>

          <!-- Actions Column -->
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let user">
              <button mat-icon-button [matMenuTriggerFor]="menu" aria-label="User actions">
                <mat-icon>more_vert</mat-icon>
              </button>
              <mat-menu #menu="matMenu">
                <!-- Only block action as per request -->
                <button mat-menu-item *ngIf="user.userStatus !== UserStatus.BLOCKED" 
                        (click)="changeUserStatus(user, UserStatus.BLOCKED)">
                  <mat-icon>block</mat-icon>
                  <span>Block User</span>
                </button>
                
                <!-- Role actions -->
                <button mat-menu-item *ngIf="user.globalRole !== GlobalRole.ADMIN && canChangeRole(user)"
                        (click)="changeUserRole(user, GlobalRole.ADMIN)">
                  <mat-icon>admin_panel_settings</mat-icon>
                  <span>Make Admin</span>
                </button>
                <button mat-menu-item *ngIf="user.globalRole !== GlobalRole.CLIENT && canChangeRole(user)"
                        (click)="changeUserRole(user, GlobalRole.CLIENT)">
                  <mat-icon>person</mat-icon>
                  <span>Make Client</span>
                </button>
                <button mat-menu-item *ngIf="user.globalRole !== GlobalRole.EMPLOYEE && canChangeRole(user)"
                        (click)="changeUserRole(user, GlobalRole.EMPLOYEE)">
                  <mat-icon>engineering</mat-icon>
                  <span>Make Employee</span>
                </button>
              </mat-menu>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .user-management-container {
      padding: 20px;
    }
    
    h2 {
      margin-bottom: 20px;
    }
    
    .loading-spinner {
      display: flex;
      justify-content: center;
      margin: 50px 0;
    }
    
    .error-message {
      color: red;
      margin: 20px 0;
      text-align: center;
    }
    
    .no-users {
      text-align: center;
      margin: 50px 0;
      color: #666;
    }
    
    .users-table-container {
      overflow-x: auto;
    }
    
    table {
      width: 100%;
    }
    
    th.mat-header-cell {
      font-weight: bold;
      color: rgba(0, 0, 0, 0.87);
    }
    
    .mat-column-actions {
      width: 100px;
      text-align: center;
    }
  `]
})
export class UserManagementComponent implements OnInit {
  users: User[] = [];
  loading = false;
  error: string | null = null;
  displayedColumns: string[] = ['id', 'name', 'username', 'email', 'role', 'status', 'actions'];
  
  // Make enums available to the template
  GlobalRole = GlobalRole;
  UserStatus = UserStatus;
  
  constructor(
    private authService: AuthService,
    private snackBar: MatSnackBar,
    private http: HttpClient
  ) {}
  
  // Manually create AdminService to avoid dependency issues
  private get adminService(): AdminService {
    return new AdminService(this.http, this.authService);
  }
  
  ngOnInit(): void {
    this.loadUsers();
  }
  
  loadUsers(): void {
    this.loading = true;
    this.error = null;
    
    this.adminService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load users. ' + (err.message || 'Unknown error');
        this.loading = false;
      }
    });
  }
  
  getStatusColor(status: UserStatus): 'primary' | 'accent' | 'warn' {
    switch (status) {
      case UserStatus.AUTHORIZED:
        return 'primary';
      case UserStatus.UNAUTHORIZED:
        return 'accent';
      case UserStatus.BLOCKED:
        return 'warn';
      default:
        return 'primary';
    }
  }
  
  canChangeRole(user: User): boolean {
    // Don't allow changing own role (for security)
    const currentUser = this.authService.getCurrentUser();
    return currentUser?.id !== user.id;
  }
  
  changeUserStatus(user: User, newStatus: UserStatus): void {
    if (user.id === this.authService.getUserId()) {
      this.snackBar.open('You cannot change your own status', 'Dismiss', {
        duration: 3000
      });
      return;
    }
    
    this.loading = true;
    this.adminService.updateUserStatus(user.id, newStatus).subscribe({
      next: () => {
        const statusText = newStatus === UserStatus.AUTHORIZED ? 'authorized' : 
                           newStatus === UserStatus.BLOCKED ? 'blocked' : 'unauthorized';
                           
        this.snackBar.open(`User ${user.firstName} ${user.lastName} successfully ${statusText}`, 'Dismiss', {
          duration: 3000
        });
        this.loadUsers(); // Reload the user list
      },
      error: (err) => {
        this.snackBar.open(`Failed to update user status: ${err.message || 'Unknown error'}`, 'Dismiss', {
          duration: 5000
        });
        this.loading = false;
      }
    });
  }
  
  changeUserRole(user: User, newRole: GlobalRole): void {
    if (user.id === this.authService.getUserId()) {
      this.snackBar.open('You cannot change your own role', 'Dismiss', {
        duration: 3000
      });
      return;
    }
    
    this.loading = true;
    this.adminService.updateUserRole(user.id, newRole).subscribe({
      next: () => {
        let roleText = 'User';
        switch (newRole) {
          case GlobalRole.ADMIN: roleText = 'Admin'; break;
          case GlobalRole.CLIENT: roleText = 'Client'; break;
          case GlobalRole.EMPLOYEE: roleText = 'Employee'; break;
        }
        
        this.snackBar.open(`User ${user.firstName} ${user.lastName} is now ${roleText}`, 'Dismiss', {
          duration: 3000
        });
        this.loadUsers(); // Reload the user list
      },
      error: (err) => {
        this.snackBar.open(`Failed to update user role: ${err.message || 'Unknown error'}`, 'Dismiss', {
          duration: 5000
        });
        this.loading = false;
      }
    });
  }
}

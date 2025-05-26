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
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']

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
    private adminService: AdminService,
    private snackBar: MatSnackBar
  ) { }

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
        this.error = 'Failed to load users: ' + (err.message || 'Unknown error');
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
    this.adminService.updateUserStatus(user.id, newStatus).subscribe({
      next: () => {
        user.userStatus = newStatus;
        this.snackBar.open(`User status changed to ${newStatus}`, 'Close', {
          duration: 3000
        });
      },
      error: (err) => {
        this.snackBar.open(`Failed to change user status: ${err.message || 'Unknown error'}`, 'Close', {
          duration: 5000,
          panelClass: 'error-snackbar'
        });
      }
    });
  }

  changeUserRole(user: User, newRole: GlobalRole): void {
    if (!this.canChangeRole(user)) {
      this.snackBar.open('You cannot change your own role for security reasons', 'Close', {
        duration: 5000,
        panelClass: 'warning-snackbar'
      });
      return;
    }
    
    // Show loading indicator or disable actions if needed
    const originalRole = user.globalRole;
    user.globalRole = newRole; // Optimistic update
    
    this.adminService.updateUserRole(user.id, newRole).subscribe({
      next: (response) => {
        // Success: role updated on the server
        this.snackBar.open(`User role changed to ${newRole}`, 'Close', {
          duration: 3000,
          panelClass: 'success-snackbar'
        });
      },
      error: (err) => {
        // Error: revert the optimistic update and show error
        user.globalRole = originalRole;
        this.snackBar.open(`Failed to change user role: ${err.message || 'Unknown error'}`, 'Close', {
          duration: 5000,
          panelClass: 'error-snackbar'
        });
      }
    });
  }
}

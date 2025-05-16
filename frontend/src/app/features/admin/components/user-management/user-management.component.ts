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
    // API endpoint for this would be needed but hasn't been implemented yet
    this.snackBar.open(`Changing user role to ${newRole} - API not implemented`, 'Close', {
      duration: 3000
    });
    
    // For demo purposes, update the local user object
    user.globalRole = newRole;
  }
}

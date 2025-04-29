import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { User } from '../../../shared/models/user.model';
import { AuthService } from '../../../shared/services/auth.service';
import { GlobalRole } from '../../../shared/enums/global-role.enum';
import { UserStatus } from '../../../shared/enums/user-status.enum';
import { RegistrationRequest } from '../../../shared/dtos/auth/registration-request.dto';
import { RegistrationResponseDTO } from '../../../shared/dtos/auth/registration-response.dto';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = 'http://localhost:8080/api/auth'; // Base URL for auth-related endpoints

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  /**
   * Get all users in the system (admin only)
   */
  getAllUsers(): Observable<User[]> {
    const userId = this.authService.getUserId();
    if (!userId) {
      return throwError(() => new Error('User ID not available'));
    }
    
    return this.http.get<User[]>(`${this.apiUrl}/all-users?userId=${userId}`);
  }

  /**
   * Register a new user (admin only)
   */
  registerUser(userData: RegistrationRequest): Observable<RegistrationResponseDTO> {
    const adminId = this.authService.getUserId();
    if (!adminId) {
      return throwError(() => new Error('Admin ID not available'));
    }
    
    return this.http.post<RegistrationResponseDTO>(`${this.apiUrl}/register?adminId=${adminId}`, userData);
  }

  /**
   * Update user status (admin only)
   */
  updateUserStatus(userId: string, status: UserStatus): Observable<string> {
    const adminId = this.authService.getUserId();
    if (!adminId) {
      return throwError(() => new Error('Admin ID not available'));
    }
    
    // Specifically for blocking, we have a dedicated endpoint
    if (status === UserStatus.BLOCKED) {
      return this.blockUser(userId);
    }

    // For other status changes, use a generic endpoint
    // Note: Using POST since this is a state change
    const endpoint = status === UserStatus.AUTHORIZED ? 'authorize-user' : 'unauthorize-user';
    return this.http.post<string>(
      `${this.apiUrl}/${endpoint}?adminId=${adminId}&userId=${userId}`, 
      {}
    );
  }

  /**
   * Update user role (admin only)
   */
  updateUserRole(userId: string, role: GlobalRole): Observable<string> {
    const adminId = this.authService.getUserId();
    if (!adminId) {
      return throwError(() => new Error('Admin ID not available'));
    }
    
    return this.http.put<string>(
      `${this.apiUrl}/update-role?adminId=${adminId}&userId=${userId}&role=${role}`,
      {}
    );
  }

  /**
   * Block a user (admin only)
   */
  private blockUser(userId: string): Observable<string> {
    const adminId = this.authService.getUserId();
    if (!adminId) {
      return throwError(() => new Error('Admin ID not available'));
    }
    
    return this.http.post<string>(
      `${this.apiUrl}/block-user?adminId=${adminId}&userId=${userId}`,
      {}
    );
  }
}

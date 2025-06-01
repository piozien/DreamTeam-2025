import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError, map, switchMap, of } from 'rxjs';
import { User } from '../models/user.model';
import { AuthService } from './auth.service';
import { GlobalRole } from '../enums/global-role.enum';
import { UserStatus } from '../enums/user-status.enum';
import { RegistrationRequest } from '../dtos/auth/registration-request.dto';
import { RegistrationResponseDTO } from '../dtos/auth/registration-response.dto';

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
    
    // Map frontend DTO to match the backend DTO field names exactly
    const requestData = {
      username: userData.username,
      firstName: userData.firstName,
      lastName: userData.lastName,
      email: userData.email,
      role: userData.globalRole.toString()  // Backend expects 'role', not 'globalRole'
    };
    
    return this.http.post<RegistrationResponseDTO>(`${this.apiUrl}/register?adminId=${adminId}`, requestData);
  }

  /**
   * Update user status (admin only)
   * This uses the available endpoints in the backend
   */
  updateUserStatus(userId: string, status: UserStatus): Observable<string> {
    const adminId = this.authService.getUserId();
    if (!adminId) {
      return throwError(() => new Error('Admin ID not available'));
    }
    
    // Different endpoints based on status
    if (status === UserStatus.BLOCKED) {
      // Get user email by userId first, since backend expects email
      return this.getUserEmailById(userId).pipe(
        switchMap((email: string | null) => {
          if (!email) {
            return throwError(() => new Error('Could not find user email'));
          }
          return this.blockUserByEmail(email);
        })
      );
    } else if (status === UserStatus.AUTHORIZED) {
      // The backend now has an endpoint for authorizing users
      return this.http.post(
        `${this.apiUrl}/authorize-user/${userId}?adminId=${adminId}`,
        {},
        { responseType: 'text' }
      ).pipe(
        map(response => response as string)
      );
    }

    // For UNAUTHORIZED status, which doesn't have an endpoint yet
    return throwError(() => new Error(`Backend endpoint for setting status to ${status} not implemented yet`));
  }

  /**
   * Get a user's email by their ID
   * Needed because blockUser endpoint expects email, not ID
   */
  private getUserEmailById(userId: string): Observable<string | null> {
    return this.getAllUsers().pipe(
      map((users: User[]) => {
        const user = users.find((u: User) => u.id === userId);
        return user ? user.email : null;
      })
    );
  }

  /**
   * Update user role (admin only)
   * Using the new backend endpoint /change-global-role
   */
  updateUserRole(userId: string, role: GlobalRole): Observable<string> {
    const adminId = this.authService.getUserId();
    if (!adminId) {
      return throwError(() => new Error('Admin ID not available'));
    }
    
    // The backend expects a request body with userId and newRole
    const requestBody = {
      userId: userId,
      newRole: role  // The backend expects 'newRole', not 'role'
    };
    
    // Use responseType: 'text' to handle plain text responses from the backend
    return this.http.post(
      `${this.apiUrl}/change-global-role?adminId=${adminId}`,
      requestBody,
      { responseType: 'text' }
    ).pipe(
      map(response => response as string)
    );
  }

  /**
   * Block a user by email (admin only)
   * This matches the backend endpoint signature exactly
   */
  private blockUserByEmail(email: string): Observable<string> {
    const adminId = this.authService.getUserId();
    if (!adminId) {
      return throwError(() => new Error('Admin ID not available'));
    }
    
    return this.http.post(
      `${this.apiUrl}/block-user?adminId=${adminId}&email=${encodeURIComponent(email)}`,
      {},
      { responseType: 'text' }
    ).pipe(
      map(response => response as string)
    );
  }
}

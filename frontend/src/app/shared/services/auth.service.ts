import { Injectable, PLATFORM_ID, Inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, map, catchError, of, throwError } from 'rxjs';
import { User } from '../models/user.model'; 
import { isPlatformBrowser } from '@angular/common';
import { jwtDecode } from 'jwt-decode'; 

import { LoginResponseDTO } from '../dtos/auth/login-response.dto';
import { RegistrationRequest } from '../dtos/auth/registration-request.dto';
import { RegistrationResponseDTO } from '../dtos/auth/registration-response.dto';
import { SetPasswordRequest } from '../dtos/auth/set-password-request.dto';
import { DecodedToken } from '../models/decoded-token.model';
import { GlobalRole } from '../enums/global-role.enum';
import { UserStatus } from '../enums/user-status.enum';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth'; 
  private tokenKey = 'authToken';

  private tokenSubject = new BehaviorSubject<string | null>(null);
  public token$ = this.tokenSubject.asObservable();

  private decodedTokenSubject = new BehaviorSubject<DecodedToken | null>(null);
  public decodedToken$ = this.decodedTokenSubject.asObservable();

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private isBrowser: boolean;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
    this.loadToken(); 
  }

  private loadToken(): void {
    if (this.isBrowser) {
      const token = localStorage.getItem(this.tokenKey);
      if (token) {
        this.updateTokenState(token);
      }
    }
  }

  private updateTokenState(token: string | null): void {
    if (token) {
      try {
        const decoded = jwtDecode<DecodedToken>(token);
        console.log('Decoded token:', JSON.stringify(decoded, null, 2)); // Log the decoded payload
        this.decodedTokenSubject.next(decoded);
        this.tokenSubject.next(token); // Set the token in the tokenSubject
        if (this.isBrowser) {
          localStorage.setItem(this.tokenKey, token);
        }
        // Create a basic user object from the decoded token
        // IMPORTANT: Ensure DecodedToken model matches the actual JWT payload from the backend
        const user: User = {
          id: decoded.userId, // Assuming 'userId' exists in the token payload
          username: decoded.sub, // 'sub' usually holds username/email
          firstName: decoded.firstName || '', // Assuming these exist
          lastName: decoded.lastName || '', // Assuming these exist
          name: `${decoded.firstName || ''} ${decoded.lastName || ''}`.trim(),
          email: decoded.email || decoded.sub, // Assuming email exists, fallback to sub
          globalRole: decoded.role || GlobalRole.USER, // Assuming 'role' exists
          userStatus: decoded.status || UserStatus.AUTHORIZED // Assuming 'status' exists
        };
        console.log('Created user object:', JSON.stringify(user, null, 2)); // Log the created user
        this.currentUserSubject.next(user); // Update the user subject
      } catch (error) {
        console.error('Failed to decode or process token:', error); // More specific error log
        this.clearTokenState();
      }
    } else {
      this.clearTokenState();
    }
  }

  private clearTokenState(): void {
    this.tokenSubject.next(null);
    this.decodedTokenSubject.next(null);
    this.currentUserSubject.next(null);
    if (this.isBrowser) {
      localStorage.removeItem(this.tokenKey);
    }
  }

  login(email: string, password: string): Observable<LoginResponseDTO> {
    return this.http.post<LoginResponseDTO>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap(response => {
        this.updateTokenState(response.token);
        // Create user object from response
        const user: User = {
          id: response.id,
          username: response.username,
          firstName: response.name.split(' ')[0],
          lastName: response.name.split(' ')[1] || '',
          name: response.name,
          email: response.email,
          globalRole: this.decodedTokenSubject.value?.role || GlobalRole.USER,
          userStatus: this.decodedTokenSubject.value?.status || UserStatus.UNAUTHORIZED
        };
        this.currentUserSubject.next(user);
      }),
      catchError(err => {
        console.error('Login failed:', err);
        this.clearTokenState(); 
        return throwError(() => new Error('Login failed')); 
      })
    );
  }

  registerUserByAdmin(request: RegistrationRequest, adminId: string): Observable<RegistrationResponseDTO> {
    return this.http.post<RegistrationResponseDTO>(`${this.apiUrl}/register?adminId=${adminId}`, request);
  }

  // Method for backwards compatibility with existing components
  register(userData: any): Observable<any> {
    const adminId = this.decodedTokenSubject.value?.userId;
    if (!adminId) {
      return throwError(() => new Error('Admin ID not available. You must be logged in as admin to register users.'));
    }
    
    const request: RegistrationRequest = {
      username: userData.userName,
      firstName: userData.firstName,
      lastName: userData.lastName,
      email: userData.email,
      globalRole: userData.globalRole || GlobalRole.USER
    };
    
    return this.registerUserByAdmin(request, adminId);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  requestPasswordReset(email: string): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/request-password-reset`, { email });
  }

  setPassword(request: SetPasswordRequest): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/set-password`, request);
  }

  logout(): void {
    this.clearTokenState();
  }

  getToken(): string | null {
    return this.tokenSubject.value;
  }

  getDecodedToken(): DecodedToken | null {
    return this.decodedTokenSubject.value;
  }

  getUserId(): string | null {
    return this.decodedTokenSubject.value?.userId ?? null;
  }

  getUserEmail(): string | null {
    return this.decodedTokenSubject.value?.sub ?? null;
  }

  getUserRole(): GlobalRole | null {
    return this.decodedTokenSubject.value?.role ?? null;
  }

  getUserStatus(): UserStatus | null {
    return this.decodedTokenSubject.value?.status ?? null;
  }

  isAuthenticated(): boolean {
    const token = this.tokenSubject.value;
    const decoded = this.decodedTokenSubject.value;
    return !!token && !!decoded && decoded.exp * 1000 > Date.now();
  }

  isAuthorized(): boolean {
    return this.isAuthenticated() && this.getUserStatus() === UserStatus.AUTHORIZED;
  }

  isAdmin(): boolean {
    return this.isAuthenticated() && this.getUserRole() === GlobalRole.ADMIN;
  }

  getAllUsers(): Observable<User[]> {
    const userId = this.getUserId(); 
    if (!userId) {
      return throwError(() => new Error('User ID not found in token. Cannot fetch users.'));
    }
    return this.http.get<User[]>(`${this.apiUrl}/all-users?userId=${userId}`);
  }

  /**
   * Initiates Google OAuth login by redirecting the main window
   * to the backend's Google authorization endpoint.
   */
  loginWithGoogle(): void {
    if (this.isBrowser) {
      // Simple redirect - the backend will handle the flow and redirect back
      // to our /auth/oauth-callback route with the token.
      window.location.href = 'http://localhost:8080/oauth2/authorization/google';
    }
  }

  /**
   * Stores the JWT token received from the OAuth callback URL
   * and updates the application's authentication state.
   * @param token The JWT token string.
   */
  storeOAuthToken(token: string): void {
    if (token) {
      this.updateTokenState(token); // Use existing method to store token and update subjects

      // After storing the token, we might want to fetch full user details
      // if the token only contains basic info, but updateTokenState
      // already decodes and sets basic user info.
      // If more details are needed, add a call here, e.g., this.fetchCurrentUserDetails();

      console.log('OAuth token stored successfully.');
      console.log(token);
    } else {
      console.error('Attempted to store an invalid OAuth token.');
      this.clearTokenState(); // Clear state if token is invalid
    }
  }

  /**
   * Processes the OAuth token received from the OAuth2 callback
   * @param token JWT token from OAuth provider
   */
  handleOAuthCallback(token: string): Observable<LoginResponseDTO> {
    // Update token state based on the OAuth response
    this.updateTokenState(token);
    
    // Return the user info from the token
    const decoded = this.getDecodedToken();
    if (!decoded) {
      return throwError(() => new Error('Invalid OAuth token'));
    }
    
    return of({
      token,
      id: this.getUserId() || '',
      email: this.getUserEmail() || '',
      name: decoded.name || '',
      username: decoded.preferred_username || ''
    });
  }
}
import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../shared/services/auth.service';
import { UserLogin } from '../../../shared/models/user.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    RouterModule, 
    HttpClientModule,
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  private router = inject(Router);
  private authService = inject(AuthService);
  
  user: UserLogin = {
    email: '', 
    password: '',
  };
  
  errorMessage: string = '';
  isLoading: boolean = false;
  isGoogleLoading: boolean = false;
  
  onSubmit(): void {
    // Reset any previous error messages
    this.errorMessage = '';
    this.isLoading = true;
    
    console.log('Login form submitted:', this.user);
    
    if (!this.user.email || !this.user.password) {
      this.errorMessage = 'Wprowadź adres email i hasło';
      this.isLoading = false;
      return;
    }
    
    this.authService.login(this.user.email, this.user.password).subscribe({
      next: () => {
        console.log('Login successful');
        this.isLoading = false;
        // Redirect to projects page after successful login
        this.router.navigate(['/projects']);
      },
      error: (error) => {
        console.error('Login failed:', error);
        this.isLoading = false;
        
        if (error.status === 500) {
          this.errorMessage = 'Błąd serwera. Spróbuj ponownie później.';
        } else if (error.status === 401) {
          this.errorMessage = 'Niepoprawne hasło.';
        } else if (error.status === 404) {
          this.errorMessage = 'Nie znaleziono użytkownika o podanym adresie email.';
        } else {
          this.errorMessage = 'Wystąpił błąd podczas logowania. Spróbuj ponownie.';
        }
      }
    });
  }

  /**
   * Initiates Google OAuth login by redirecting to the backend's OAuth endpoint
   */
  loginWithGoogle(): void {
    this.isGoogleLoading = true;
    this.errorMessage = '';
    
    // Call the service method that will redirect to Google OAuth
    this.authService.loginWithGoogle();
  }
}

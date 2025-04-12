import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { AuthService } from '../../../shared/services/auth.service';
import { UserCreate, GlobalRole } from '../../../shared/models/user.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HttpClientModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private router = inject(Router);
  private authService = inject(AuthService);
  
  user: UserCreate = {
    email: '', 
    password: '',
    userName: '',
    firstName: '',
    lastName: '',
  };
  
  errorMessage: string = '';
  isLoading: boolean = false;

  onSubmit(): void {
    // Reset any previous error messages
    this.errorMessage = '';
    this.isLoading = true;
    
    console.log('Form submitted:', this.user);
    
    // Basic form validation
    if (!this.user.email || !this.user.password || !this.user.userName || !this.user.firstName || !this.user.lastName) {
      this.errorMessage = 'Wypełnij wszystkie wymagane pola';
      this.isLoading = false;
      return;
    }
    
    this.authService.register(
      this.user.userName, 
      this.user.firstName, 
      this.user.lastName, 
      this.user.password, 
      this.user.email
    ).subscribe({
      next: () => {
        console.log('Registration successful');
        this.isLoading = false;
        this.router.navigate(['/']);
      },
      error: (error) => {
        console.error('Register failed:', error);
        this.isLoading = false;
        
        if (error.status === 500) {
          this.errorMessage = 'Błąd serwera. Spróbuj ponownie później.';
        } else if (error.status === 409) {
          this.errorMessage = 'Użytkownik o podanym adresie email już istnieje.';
        } else if (error.status === 400) {
          this.errorMessage = 'Nieprawidłowe dane. Sprawdź wprowadzone informacje.';
        } else {
          this.errorMessage = 'Wystąpił błąd podczas rejestracji. Spróbuj ponownie.';
        }
      }
    });
  }
}

import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { AuthService } from '../../../shared/services/auth.service';
import { UserCreate } from '../../../shared/models/user.model';
import { GlobalRole } from '../../../shared/enums/global-role.enum';

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
    username: '',
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
    if (!this.user.email || !this.user.password || !this.user.username || !this.user.firstName || !this.user.lastName) {
      this.errorMessage = 'Wypełnij wszystkie wymagane pola';
      this.isLoading = false;
      return;
    }
    
    // Create an object matching what the register method expects (for compatibility)
    const registerData = {
      userName: this.user.username,
      firstName: this.user.firstName,
      lastName: this.user.lastName,
      password: this.user.password,
      email: this.user.email,
      globalRole: GlobalRole.CLIENT
    };
    
    this.authService.register(registerData).subscribe({
      next: () => {
        console.log('Registration successful');
        this.isLoading = false;
        this.router.navigate(['/auth/login']);
      },
      error: (error: any) => {
        console.error('Registration failed:', error);
        this.isLoading = false;
        
        if (error.status === 400) {
          this.errorMessage = 'Nieprawidłowe dane - sprawdź wszystkie pola';
        } else if (error.status === 409) {
          this.errorMessage = 'Użytkownik o podanym adresie email już istnieje';
        } else if (error.status === 403) {
          this.errorMessage = 'Tylko administrator może tworzyć nowych użytkowników';
        } else {
          this.errorMessage = 'Błąd rejestracji. Spróbuj ponownie później.';
        }
      }
    });
  }
}

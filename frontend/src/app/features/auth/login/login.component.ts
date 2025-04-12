import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { AuthService } from '../../../shared/services/auth.service';
import { UserLogin} from '../../../shared/models/user.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, HttpClientModule],
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
        this.router.navigate(['/']);
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
}

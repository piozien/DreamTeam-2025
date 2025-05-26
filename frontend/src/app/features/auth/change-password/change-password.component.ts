import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../shared/services/auth.service';
import { SetPasswordRequest } from '../../../shared/dtos/auth/set-password-request.dto';
import { ToastNotificationService } from '../../../shared/services/toast-notification.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './change-password.component.html',
  styleUrl: './change-password.component.scss'
})
export class ChangePasswordComponent implements OnInit {
  passwordForm!: FormGroup;
  loading = false;
  hidePassword = true;
  hideConfirmPassword = true;
  errorMessage = '';
  
  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private toastr: ToastNotificationService
  ) {}
  
  ngOnInit(): void {
    // Initialize form
    this.passwordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    });
    
    // Add custom validator
    this.passwordForm.addValidators(
      (group: AbstractControl): ValidationErrors | null => {
        const pass = group.get('newPassword')?.value;
        const confirmPass = group.get('confirmPassword')?.value;
        
        if (pass !== confirmPass) {
          this.passwordForm.get('confirmPassword')?.setErrors({ passwordMismatch: true });
          return { passwordMismatch: true };
        }
        return null;
      }
    );
    
    // Check if user is authenticated
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
    }
  }
  
  onSubmit(): void {
    // Mark all fields as touched to trigger validation messages
    this.passwordForm.markAllAsTouched();
    
    if (this.passwordForm.invalid) {
      return;
    }
    
    this.loading = true;
    this.errorMessage = '';
    
    const email = this.authService.getUserEmail();
    if (!email) {
      this.errorMessage = 'Nie udało się pobrać adresu email użytkownika';
      this.loading = false;
      return;
    }
    
    const request: SetPasswordRequest = {
      email: email,
      newPassword: this.passwordForm.get('newPassword')?.value
    };
    
    this.authService.setPassword(request).subscribe({
      next: () => {
        this.loading = false;
        // Success will be handled in the UI without toast notification
        this.router.navigate(['/projects']);
      },
      error: (error) => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Wystąpił błąd podczas zmiany hasła';
        // Error will be set in the UI without toast notification
        console.error('Password change error:', error);
      }
    });
  }
}

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AdminService } from '../../../../shared/services/admin.service';
import { GlobalRole } from '../../../../shared/enums/global-role.enum';
import { RegistrationRequest } from '../../../../shared/dtos/auth/registration-request.dto';

@Component({
  selector: 'app-register-user',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatSelectModule,
    MatSnackBarModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="register-container">
      <h2>Register New User</h2>
      
      <mat-card class="register-card mat-elevation-z4">
        <mat-card-content>
          <form [formGroup]="registerForm" (ngSubmit)="onRegister()">
            <mat-form-field appearance="outline" class="form-field">
              <mat-label>First Name</mat-label>
              <input matInput formControlName="firstName" placeholder="First Name" required>
              <mat-error *ngIf="registerForm.get('firstName')?.hasError('required')">
                First name is required
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Last Name</mat-label>
              <input matInput formControlName="lastName" placeholder="Last Name" required>
              <mat-error *ngIf="registerForm.get('lastName')?.hasError('required')">
                Last name is required
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" placeholder="Username" required>
              <mat-error *ngIf="registerForm.get('username')?.hasError('required')">
                Username is required
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Email</mat-label>
              <input matInput formControlName="email" placeholder="Email" required type="email">
              <mat-error *ngIf="registerForm.get('email')?.hasError('required')">
                Email is required
              </mat-error>
              <mat-error *ngIf="registerForm.get('email')?.hasError('email')">
                Please enter a valid email address
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="form-field">
              <mat-label>Role</mat-label>
              <mat-select formControlName="globalRole" required>
                <mat-option [value]="GlobalRole.CLIENT">Client</mat-option>
                <mat-option [value]="GlobalRole.EMPLOYEE">Employee</mat-option>
                <mat-option [value]="GlobalRole.ADMIN">Admin</mat-option>
              </mat-select>
              <mat-error *ngIf="registerForm.get('globalRole')?.hasError('required')">
                User role is required
              </mat-error>
            </mat-form-field>

            <div class="form-actions">
              <button type="button" mat-stroked-button (click)="resetForm()" [disabled]="loading">
                Reset
              </button>
              <button type="submit" mat-raised-button color="primary" [disabled]="registerForm.invalid || loading">
                <mat-spinner *ngIf="loading" diameter="20" class="spinner-button"></mat-spinner>
                <span *ngIf="!loading">Register User</span>
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>

      <div *ngIf="success" class="success-message">
        <p>{{ successMessage }}</p>
        <p>An email will be sent to the user with instructions to set their password.</p>
      </div>
    </div>
  `,
  styles: [`
    .register-container {
      padding: 20px;
      max-width: 600px;
      margin: 0 auto;
    }
    
    h2 {
      margin-bottom: 20px;
    }
    
    .form-field {
      width: 100%;
      margin-bottom: 20px;
    }
    
    .form-actions {
      display: flex;
      justify-content: space-between;
      margin-top: 30px;
    }
    
    .success-message {
      margin-top: 20px;
      padding: 15px;
      background-color: #dff0d8;
      border-radius: 4px;
      color: #3c763d;
    }
    
    .register-card {
      padding: 10px;
    }
    
    .spinner-button {
      display: inline-block;
      vertical-align: middle;
      margin-right: 5px;
    }
  `]
})
export class RegisterUserComponent implements OnInit {
  registerForm!: FormGroup;
  loading = false;
  success = false;
  successMessage = '';
  
  // Make enum available to template
  GlobalRole = GlobalRole;
  
  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private snackBar: MatSnackBar
  ) {}
  
  ngOnInit(): void {
    this.initForm();
  }
  
  initForm(): void {
    this.registerForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      globalRole: [GlobalRole.CLIENT, Validators.required]
    });
  }
  
  onRegister(): void {
    if (this.registerForm.invalid) {
      return;
    }
    
    this.loading = true;
    this.success = false;
    
    const formValue = this.registerForm.value;
    const registrationRequest: RegistrationRequest = {
      firstName: formValue.firstName,
      lastName: formValue.lastName,
      username: formValue.username,
      email: formValue.email,
      globalRole: formValue.globalRole
    };
    
    this.adminService.registerUser(registrationRequest).subscribe({
      next: (response) => {
        this.loading = false;
        this.success = true;
        this.successMessage = response.message;
        this.resetForm();
      },
      error: (err) => {
        this.loading = false;
        this.snackBar.open(
          `Registration failed: ${err.error?.message || err.message || 'Unknown error'}`,
          'Dismiss',
          { duration: 5000 }
        );
      }
    });
  }
  
  resetForm(): void {
    this.registerForm.reset({
      globalRole: GlobalRole.CLIENT
    });
  }
}

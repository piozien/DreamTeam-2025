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
  templateUrl: './register-user.component.html',
  styleUrls: ['./register-user.component.scss']
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

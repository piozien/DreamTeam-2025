import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ProjectUserRole } from '../../../../shared/models/project.model'
import { AuthService } from '../../../../shared/services/auth.service';
import { User } from '../../../../shared/models/user.model';

@Component({
  selector: 'app-member-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <div *ngIf="loading" class="spinner-container">
        <mat-spinner diameter="40"></mat-spinner>
      </div>
      <form #memberForm="ngForm" (ngSubmit)="onSubmit()" *ngIf="!loading">
        <ng-container *ngIf="data.mode === 'add'">
          <mat-form-field appearance="fill" class="full-width">
            <mat-label>Użytkownik</mat-label>
            <mat-select [(ngModel)]="selectedUserId" name="userId" required>
              <mat-option *ngFor="let user of users" [value]="user.id">
                {{ user.firstName }} {{ user.lastName }} ({{ user.email }})
              </mat-option>
            </mat-select>
          </mat-form-field>
        </ng-container>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Rola</mat-label>
          <mat-select [(ngModel)]="selectedRole" name="role" required>
            <mat-option [value]="ProjectUserRole.PM">Manager Projektu</mat-option>
            <mat-option [value]="ProjectUserRole.MEMBER">Członek</mat-option>
            <mat-option [value]="ProjectUserRole.CLIENT">Klient</mat-option>
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Anuluj</button>
      <button mat-raised-button color="primary" 
              (click)="onSubmit()" 
              [disabled]="!isValid()">
        {{ data.submitButton }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
      max-width: 500px;
    }
    .full-width {
      width: 100%;
      margin-bottom: 15px;
    }
    mat-dialog-content {
      padding-top: 20px;
    }
    .spinner-container {
      display: flex;
      justify-content: center;
      padding: 20px 0;
    }
  `]
})
export class MemberDialogComponent implements OnInit {
  ProjectUserRole = ProjectUserRole; // For template access
  selectedUserId: string = '';
  selectedRole: ProjectUserRole = ProjectUserRole.MEMBER;
  users: User[] = [];
  loading: boolean = false;

  constructor(
    public dialogRef: MatDialogRef<MemberDialogComponent>,
    private authService: AuthService,
    @Inject(MAT_DIALOG_DATA) public data: { 
      title: string; 
      submitButton: string;
      mode: 'add' | 'edit';
      userId?: string;
      currentRole?: ProjectUserRole;
    }
  ) {}

  ngOnInit(): void {
    if (this.data.mode === 'edit' && this.data.currentRole) {
      this.selectedRole = this.data.currentRole;
    }

    if (this.data.mode === 'edit' && this.data.userId) {
      this.selectedUserId = this.data.userId;
    }

    if (this.data.mode === 'add') {
      this.loadUsers();
    }
  }

  loadUsers(): void {
    this.loading = true;
    this.authService.getAllUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.loading = false;
      }
    });
  }

  isValid(): boolean {
    if (this.data.mode === 'add') {
      return !!this.selectedUserId && !!this.selectedRole;
    }
    return !!this.selectedRole;
  }

  onSubmit(): void {
    if (this.isValid()) {
      if (this.data.mode === 'add') {
        console.log('Submitting user ID:', this.selectedUserId);
        this.dialogRef.close({
          userId: this.selectedUserId,
          role: this.selectedRole
        });
      } else {
        this.dialogRef.close({
          role: this.selectedRole
        });
      }
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}

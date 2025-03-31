import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE, MatNativeDateModule } from '@angular/material/core';
import { Project } from '../../../../shared/models/project.model';

const MY_DATE_FORMATS = {
  parse: {
    dateInput: 'DD/MM/YYYY',
  },
  display: {
    dateInput: 'DD/MM/YYYY',
    monthYearLabel: 'MMM YYYY',
    dateA11yLabel: 'LL',
    monthYearA11yLabel: 'MMMM YYYY',
  },
};

@Component({
  selector: 'app-project-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'pl-PL' },
    { provide: MAT_DATE_FORMATS, useValue: MY_DATE_FORMATS }
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <form #projectForm="ngForm" (ngSubmit)="onSubmit()">
        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Nazwa projektu</mat-label>
          <input matInput [(ngModel)]="project.name" name="name" required>
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Opis</mat-label>
          <textarea matInput [(ngModel)]="project.description" name="description" required></textarea>
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Data rozpoczęcia</mat-label>
          <input matInput [matDatepicker]="startPicker" [(ngModel)]="project.startDate" 
                 name="startDate" required (dateChange)="onStartDateChange()">
          <mat-datepicker-toggle matSuffix [for]="startPicker"></mat-datepicker-toggle>
          <mat-datepicker #startPicker></mat-datepicker>
        </mat-form-field>

        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Data zakończenia</mat-label>
          <input matInput [matDatepicker]="endPicker" [(ngModel)]="project.endDate" 
                 name="endDate" required [min]="project.startDate" 
                 #endDate="ngModel">
          <mat-datepicker-toggle matSuffix [for]="endPicker"></mat-datepicker-toggle>
          <mat-datepicker #endPicker></mat-datepicker>
          <mat-error *ngIf="endDate.errors?.['matDatepickerMin']">
            Data zakończenia nie może być wcześniejsza niż data rozpoczęcia
          </mat-error>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Anuluj</button>
      <button mat-raised-button color="primary" 
              (click)="onSubmit()" 
              [disabled]="!projectForm.form.valid || !isValidDateRange()">
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
    mat-error {
      font-size: 12px;
      margin-top: 4px;
    }
  `]
})
export class ProjectDialogComponent {
  project: Partial<Project> = {
    name: '',
    description: '',
    startDate: new Date(),
    endDate: new Date(new Date().setDate(new Date().getDate() + 1)), // Set default end date to tomorrow
    members: []
  };

  constructor(
    public dialogRef: MatDialogRef<ProjectDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { title: string; submitButton: string }
  ) {}

  onStartDateChange(): void {
    // If end date is before start date, set it to start date + 1 day
    if (this.project.endDate && this.project.startDate && 
        this.project.endDate < this.project.startDate) {
      this.project.endDate = new Date(new Date(this.project.startDate).setDate(this.project.startDate.getDate() + 1));
    }
  }

  isValidDateRange(): boolean {
    if (!this.project.startDate || !this.project.endDate) {
      return false;
    }
    return this.project.endDate >= this.project.startDate;
  }

  onSubmit(): void {
    if (this.isValidDateRange()) {
      this.dialogRef.close(this.project);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}

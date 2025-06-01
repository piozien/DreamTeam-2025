import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DATE_FORMATS, MAT_DATE_LOCALE, MatNativeDateModule } from '@angular/material/core';
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
  templateUrl: './project-dialog.component.html',
  styleUrls: ['./project-dialog.component.scss']
})
export class ProjectDialogComponent {
  project: Partial<Project> = {
    name: '',
    description: '',
    startDate: new Date(),
  };
  
  // Track the original start date for editing mode
  originalStartDate: Date | null = null;
  
  today = new Date();

  // Date filter to disable all dates before today (except in edit mode)
  dateFilter = (date: Date | null): boolean => {
    if (!date) return false;
    const currentDate = new Date(date);
    
    // In edit mode, allow dates that are not earlier than original start date
    if (this.data.isEditMode && this.originalStartDate) {
      // Create a copy of the date and set to start of day for comparison
      const originalDate = new Date(this.originalStartDate);
      originalDate.setHours(0, 0, 0, 0);
      
      // Don't allow choosing dates earlier than the original
      return currentDate >= originalDate;
    }
    
    // For new projects, don't allow dates before today
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return currentDate >= today;
  };
  
  // Date filter for end date to ensure it's after start date
  endDateFilter = (date: Date | null): boolean => {
    if (!date) return false;
    if (!this.project.startDate) return true; // If no start date, allow any date
    
    const endDate = new Date(date);
    const startDate = new Date(this.project.startDate);
    return endDate >= startDate;
  };

  constructor(
    public dialogRef: MatDialogRef<ProjectDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { title: string; submitButton: string; isEditMode?: boolean }
  ) {
    // Reset hours to start of day for date comparison
    this.today.setHours(0, 0, 0, 0);
    
    // Initialize start date to today only for new projects
    if (!data.isEditMode) {
      // Initialize start date to today to avoid starting with invalid date
      this.project.startDate = new Date(this.today);
    }
  }
  
  // Method to set the original start date when pre-filling in edit mode
  // This should be called when setting project data in edit mode
  setOriginalStartDate(startDate: Date): void {
    if (this.data.isEditMode) {
      this.originalStartDate = new Date(startDate);
    }
  }

  onStartDateChange(): void {
    // If end date is before start date, set it to start date + 1 day
    if (this.project.endDate && this.project.startDate && 
        this.project.endDate < this.project.startDate) {
      this.project.endDate = new Date(new Date(this.project.startDate).setDate(this.project.startDate.getDate() + 1));
    }
  }

  isValidDateRange(): boolean {
    if (!this.project.startDate) {
      return false;
    }
    
    // Check if start date is not before today (except in edit mode)
    let startDateValid = true;
    if (!this.data.isEditMode) {
      startDateValid = this.project.startDate >= this.today;
    }
    
    // Check if end date is after start date (if end date exists)
    const endDateValid = !this.project.endDate || this.project.endDate >= this.project.startDate;
    
    return startDateValid && endDateValid;
  }
  
  isStartDateValid(): boolean {
    if (!this.project.startDate) return false;
    
    // In edit mode, allow any start date including those in the past
    if (this.data.isEditMode) {
      return true;
    }
    
    // For new projects, don't allow dates before today
    return this.project.startDate >= this.today;
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

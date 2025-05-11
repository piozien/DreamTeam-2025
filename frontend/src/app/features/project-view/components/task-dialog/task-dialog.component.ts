import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatSelectModule } from '@angular/material/select';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE, MatNativeDateModule } from '@angular/material/core';
import { Task, TaskPriority, TaskStatus } from '../../../../shared/models/task.model';

export interface TaskDialogData {
  title: string;
  submitButton: string;
  projectId: string;
  task?: Partial<Task>;
}

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
  selector: 'app-task-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSelectModule
  ],
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'pl-PL' },
    { provide: MAT_DATE_FORMATS, useValue: MY_DATE_FORMATS }
  ],
  templateUrl: './task-dialog.component.html',
  styleUrls: ['./task-dialog.component.scss']
})
export class TaskDialogComponent {
  task: Partial<Task> = {
    name: '',
    description: '',
    startDate: new Date().toISOString().split('T')[0],
    priority: TaskPriority.IMPORTANT,
    status: TaskStatus.TO_DO
  };
  
  // Create Date objects for date pickers
  startDateObj: Date = new Date();
  endDateObj: Date | null = null;
  
  taskPriorities = Object.values(TaskPriority);
  taskStatuses = Object.values(TaskStatus);
  today = new Date();

  // Date filter to disable all dates before today
  dateFilter = (date: Date | null): boolean => {
    if (!date) return false;
    const currentDate = new Date(date);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return currentDate >= today;
  };
  
  // Date filter for end date to ensure it's after start date
  endDateFilter = (date: Date | null): boolean => {
    if (!date) return false;
    if (!this.task.startDate) return true; // If no start date, allow any date
    
    const endDate = new Date(date);
    const startDate = new Date(this.task.startDate);
    return endDate >= startDate;
  };

  constructor(
    public dialogRef: MatDialogRef<TaskDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TaskDialogData
  ) {
    // Reset hours to start of day for date comparison
    this.today.setHours(0, 0, 0, 0);
    
    // Initialize start date to today to avoid starting with invalid date
    this.task.startDate = this.formatDateToString(this.today);
    this.startDateObj = new Date(this.today);
    
    // If we're editing an existing task, populate the form
    if (data.task) {
      this.task = { ...data.task };
      
      // Create Date objects for the date pickers based on string dates
      if (this.task.startDate) {
        this.startDateObj = new Date(this.task.startDate);
      }
      
      if (this.task.endDate) {
        this.endDateObj = new Date(this.task.endDate);
      }
    }
  }

  // Handle changes to the start date picker
  onStartDateChange(): void {
    // Update the string date in the task model
    this.task.startDate = this.formatDateToString(this.startDateObj);
    
    // If end date is before start date, set it to start date + 1 day
    if (this.endDateObj && this.startDateObj) {
      if (this.endDateObj < this.startDateObj) {
        const newEndDate = new Date(this.startDateObj);
        newEndDate.setDate(this.startDateObj.getDate() + 1);
        this.endDateObj = newEndDate;
        this.task.endDate = this.formatDateToString(newEndDate);
      }
    }
  }

  isValidDateRange(): boolean {
    if (!this.startDateObj) {
      return false;
    }
    
    // Check if start date is not before today
    const startDateValid = this.startDateObj >= this.today;
    
    // Check if end date is after start date (if end date exists)
    let endDateValid = true;
    if (this.endDateObj) {
      endDateValid = this.endDateObj >= this.startDateObj;
    }
    
    return startDateValid && endDateValid;
  }
  
  isStartDateValid(): boolean {
    if (!this.startDateObj) return false;
    return this.startDateObj >= this.today;
  }
  
  // Helper method to format a Date object to YYYY-MM-DD string
  formatDateToString(date: Date): string {
    if (!date) return '';
    // Use local timezone to avoid date shifting due to UTC conversion
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
  
  // Handle changes to the end date picker
  onEndDateChange(): void {
    if (this.endDateObj) {
      this.task.endDate = this.formatDateToString(this.endDateObj);
    } else {
      this.task.endDate = undefined;
    }
  }

  onSubmit(): void {
    if (this.isValidDateRange()) {
      // Ensure dates are in the correct string format
      this.task.startDate = this.formatDateToString(this.startDateObj);
      if (this.endDateObj) {
        this.task.endDate = this.formatDateToString(this.endDateObj);
      }
      
      // Create a copy of the task to send
      const taskToSubmit: Partial<Task> = {
        ...this.task,
        projectId: this.data.projectId
      };
      
      this.dialogRef.close(taskToSubmit);
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
  
  getPriorityDisplayName(priority: TaskPriority): string {
    switch (priority) {
      case TaskPriority.OPTIONAL: return 'Opcjonalny';
      case TaskPriority.IMPORTANT: return 'Ważny';
      case TaskPriority.CRITICAL: return 'Krytyczny';
      default: return priority;
    }
  }

  getStatusDisplayName(status: TaskStatus): string {
    switch (status) {
      case TaskStatus.TO_DO: return 'Do zrobienia';
      case TaskStatus.IN_PROGRESS: return 'W trakcie';
      case TaskStatus.FINISHED: return 'Zakończone';
      default: return status;
    }
  }
}

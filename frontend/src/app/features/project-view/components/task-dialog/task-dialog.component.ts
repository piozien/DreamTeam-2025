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
  projectEndDate?: string; // Project end date to limit task end date
  task?: Partial<Task>;
  isEditMode?: boolean; // Flag to indicate edit mode
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
  
  // Time strings for time inputs (HH:MM format)
  startTimeString: string = '09:00';
  endTimeString: string = '17:00';
  
  taskPriorities = Object.values(TaskPriority);
  taskStatuses = Object.values(TaskStatus);
  today = new Date();

  // Store project end date if provided
  projectEndDate: Date | null = null;
  
  // Date filter for start date
  dateFilter = (date: Date | null): boolean => {
    if (!date) return false;
    const currentDate = new Date(date);
    
    // First check if date is within project bounds
    if (this.projectEndDate) {
      // Don't allow task start date to be after project end date
      if (currentDate > this.projectEndDate) {
        return false;
      }
    }
    
    // In edit mode, we should allow keeping the original start date even if it's in the past
    if (this.data.isEditMode && this.task.id) {
      return true;
    }
    
    // For new tasks, don't allow dates before today
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return currentDate >= today;
  };
  
  // Check if start date is valid
  isStartDateValid(): boolean {
    if (!this.startDateObj) return false;
    
    // In edit mode, we allow any start date
    if (this.data.isEditMode) return true;
    
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return this.startDateObj >= today;
  }
  
  // Check if start date is after project end date
  isStartDateAfterProjectEndDate(): boolean {
    if (!this.startDateObj || !this.projectEndDate) return false;
    
    // Compare dates without time
    const startDate = new Date(this.startDateObj);
    startDate.setHours(0, 0, 0, 0);
    
    const projectEnd = new Date(this.projectEndDate);
    projectEnd.setHours(23, 59, 59, 999); // End of the project end day
    
    return startDate > projectEnd;
  }
  
  // Date filter for end date to ensure it's after start date and before project end date
  endDateFilter = (date: Date | null): boolean => {
    if (!date) return false;
    
    const endDate = new Date(date);
    let isValid = true;
    
    // Check if after start date
    if (this.startDateObj) {
      isValid = endDate >= this.startDateObj;
    }
    
    // Check if before project end date (if project end date is defined)
    if (isValid && this.projectEndDate) {
      isValid = endDate <= this.projectEndDate;
    }
    
    return isValid;
  };

  constructor(
    public dialogRef: MatDialogRef<TaskDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TaskDialogData
  ) {
    // Reset hours to start of day for date comparison
    this.today.setHours(0, 0, 0, 0);
    
    // Initialize projectEndDate if provided
    if (data.projectEndDate) {
      this.projectEndDate = new Date(data.projectEndDate);
    }
    
    // Default behavior for new tasks
    if (!data.isEditMode) {
      // Initialize start date to today to avoid starting with invalid date
      this.task.startDate = this.formatDateToString(this.today);
      this.startDateObj = new Date(this.today);
    }
    
    // If we're editing an existing task, populate the form
    if (data.task) {
      this.task = { ...data.task };
      
      // Create Date objects for the date pickers based on string dates
      if (this.task.startDate) {
        this.startDateObj = new Date(this.task.startDate);
        // Extract time from the ISO string if available
        const startTime = this.task.startDate.split('T')[1];
        if (startTime) {
          this.startTimeString = startTime.substring(0, 5); // Get HH:MM part
        }
      }
      
      if (this.task.endDate) {
        this.endDateObj = new Date(this.task.endDate);
        // Extract time from the ISO string if available
        const endTime = this.task.endDate.split('T')[1];
        if (endTime) {
          this.endTimeString = endTime.substring(0, 5); // Get HH:MM part
        }
      }
    }
  }

  // Handle changes to the start date picker
  onStartDateChange(): void {
    // Update the string date in the task model with the combined date and time
    this.updateStartDateTime();
    
    // If end date is before start date, set it to start date + 1 day
    if (this.endDateObj && this.startDateObj) {
      if (this.endDateObj < this.startDateObj) {
        const newEndDate = new Date(this.startDateObj);
        newEndDate.setDate(this.startDateObj.getDate() + 1);
        this.endDateObj = newEndDate;
        this.updateEndDateTime();
      }
    }
  }
  
  // Handle changes to the start time picker
  onStartTimeChange(): void {
    this.updateStartDateTime();
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
  
  // isStartDateValid is already implemented above
  
  // Helper method to format a Date object to YYYY-MM-DD string (without time)
  formatDateToString(date: Date): string {
    if (!date) return '';
    // Use local timezone to avoid date shifting due to UTC conversion
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
  
  // Helper method to format a Date object to ISO string with time
  formatDateTimeToString(date: Date, timeString: string): string {
    if (!date) return '';
    
    // Get the date part in YYYY-MM-DD format
    const datePart = this.formatDateToString(date);
    
    // Combine with the time string
    return `${datePart}T${timeString}:00`;
  }
  
  // Update the start date and time in the task model
  updateStartDateTime(): void {
    if (this.startDateObj) {
      this.task.startDate = this.formatDateTimeToString(this.startDateObj, this.startTimeString);
    }
  }
  
  // Update the end date and time in the task model
  updateEndDateTime(): void {
    if (this.endDateObj) {
      this.task.endDate = this.formatDateTimeToString(this.endDateObj, this.endTimeString);
    } else {
      this.task.endDate = undefined;
    }
  }
  
  // Handle changes to the end date picker
  onEndDateChange(): void {
    this.updateEndDateTime();
  }
  
  // Handle changes to the end time picker
  onEndTimeChange(): void {
    if (this.endDateObj) {
      this.updateEndDateTime();
    }
  }

  onSubmit(): void {
    if (this.isValidDateRange()) {
      // Ensure dates are in the correct string format with time
      this.updateStartDateTime();
      if (this.endDateObj) {
        this.updateEndDateTime();
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

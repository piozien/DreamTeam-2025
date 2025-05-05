import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { Task, TaskPriority, TaskStatus } from '../../../../shared/models/task.model';

export interface TaskDialogData {
  title: string;
  submitButton: string;
  projectId: string;
}

@Component({
  selector: 'app-task-dialog',
  templateUrl: './task-dialog.component.html',
  styleUrls: ['./task-dialog.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule
  ]
})
export class TaskDialogComponent implements OnInit {
  taskForm!: FormGroup;
  task: Partial<Task> = {};
  taskPriorities = Object.values(TaskPriority);
  taskStatuses = Object.values(TaskStatus);
  minDate = new Date(); // Minimum date is today

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<TaskDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TaskDialogData
  ) {}

  ngOnInit(): void {
    this.taskForm = this.fb.group({
      name: [this.task.name || '', [Validators.required]],
      description: [this.task.description || ''],
      startDate: [this.task.startDate ? new Date(this.task.startDate) : new Date(), [Validators.required]],
      endDate: [this.task.endDate ? new Date(this.task.endDate) : null],
      priority: [this.task.priority || TaskPriority.IMPORTANT, [Validators.required]],
      status: [this.task.status || TaskStatus.TO_DO, [Validators.required]]
    });
    
    // Set up subscription to start date changes to adjust end date if needed
    this.taskForm.get('startDate')?.valueChanges.subscribe(newStartDate => {
      this.onStartDateChange(newStartDate);
    });
  }

  /**
   * Handles start date changes
   * If end date is before start date, sets end date to start date + 1 day
   */
  onStartDateChange(newStartDate: Date): void {
    if (!newStartDate) return;
    
    const currentEndDate = this.taskForm.get('endDate')?.value;
    // If end date exists and is before the new start date
    if (currentEndDate && new Date(currentEndDate) < new Date(newStartDate)) {
      // Create new date that's one day after the start date
      const newEndDate = new Date(newStartDate);
      newEndDate.setDate(newEndDate.getDate() + 1);
      this.taskForm.get('endDate')?.setValue(newEndDate);
    }
  }

  onSubmit(): void {
    if (this.taskForm.valid) {
      const formValue = this.taskForm.value;
      
      // Format dates properly without timezone issues
      // Use a helper function to extract the exact date in YYYY-MM-DD format without timezone issues
      const getLocalDateString = (date: Date): string => {
        return date ? `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` : '';
      };
      
      const result = {
        projectId: this.data.projectId,
        name: formValue.name,
        description: formValue.description || '',
        startDate: getLocalDateString(formValue.startDate),
        endDate: formValue.endDate ? getLocalDateString(formValue.endDate) : null,
        priority: formValue.priority,
        status: formValue.status,
        assigneeIds: [],  // Initialize as empty arrays to match backend expectation
        comments: [],
        files: [],
        dependencyIds: []
      };
      
      console.log('Sending task with dates:', result);
      this.dialogRef.close(result);
    }
  }

  onCancel(): void {
    this.dialogRef.close(null);
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

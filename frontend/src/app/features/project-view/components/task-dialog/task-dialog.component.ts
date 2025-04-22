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
  }

  onSubmit(): void {
    if (this.taskForm.valid) {
      const formValue = this.taskForm.value;
      
      // Format dates to ISO strings and ensure structure matches TaskRequestDTO
      const result = {
        projectId: this.data.projectId,
        name: formValue.name,
        description: formValue.description || '',
        startDate: formValue.startDate.toISOString().split('T')[0],
        endDate: formValue.endDate ? formValue.endDate.toISOString().split('T')[0] : null,
        priority: formValue.priority,
        status: formValue.status,
        assigneeIds: [],  // Initialize as empty arrays to match backend expectation
        comments: [],
        files: [],
        dependencyIds: []
      };
      
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

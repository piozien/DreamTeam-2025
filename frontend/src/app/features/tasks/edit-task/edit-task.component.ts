import { Component, OnInit, inject } from '@angular/core';
import { ToastNotificationService } from '../../../shared/services/toast-notification.service';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatDividerModule } from '@angular/material/divider';
import { TaskService } from '../../../shared/services/task.service';
import { Task, TaskUpdate, TaskAssignee } from '../../../shared/models/task.model';
import { TaskPriority } from '../../../shared/enums/task-priority.enum';
import { TaskStatus } from '../../../shared/enums/task-status.enum';
import { AuthService } from '../../../shared/services/auth.service';
import { ProjectService } from '../../../shared/services/project.service';
import { ProjectMemberDTO, Project } from '../../../shared/models/project.model';

@Component({
  selector: 'app-edit-task',
  templateUrl: './edit-task.component.html',
  styleUrls: ['./edit-task.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatChipsModule,
    MatIconModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTabsModule,
    MatDividerModule
  ],
  providers: [DatePipe]
})
export class EditTaskComponent implements OnInit {
  taskId!: string;
  projectId!: string;
  task: Task | null = null;
  taskForm!: FormGroup;
  loading = true;
  submitting = false;
  
  // For assignees - project members that can be assigned to tasks
  availableUsers: { id: string; name: string; }[] = []; // Simplified user objects with just id and name
  selectedAssigneeId: string | null = null;
  
  // Date and time handling for validation
  projectStartDate: Date | null = null;
  projectEndDate: Date | null = null;
  originalStartDate: Date | null = null;
  originalEndDate: Date | null = null;
  minStartDate: Date | null = null;
  
  // Time selection for 24-hour format
  hours: string[] = Array.from({ length: 24 }, (_, i) => i.toString().padStart(2, '0'));
  minutes: string[] = Array.from({ length: 60 }, (_, i) => i.toString().padStart(2, '0'));
  
  // Individual time components
  startHour: string = '09';
  startMinute: string = '00';
  endHour: string = '17';
  endMinute: string = '00';
  
  // Time strings (HH:MM format)
  startTimeString: string = '09:00'; // Default start time
  endTimeString: string = '17:00';   // Default end time
  
  // For dependencies
  availableTasks: Task[] = [];
  selectedDependencyId: string | null = null;
  
  // For comments
  newComment: string = '';
  
  // Map for user ID to name lookup
  userMap: Map<string, string> = new Map();
  // Map for task ID to name lookup
  taskMap: Map<string, string> = new Map();
  
  // Enums for template
  TaskStatus = TaskStatus;
  TaskPriority = TaskPriority;
  
  // Use inject for services
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private taskService = inject(TaskService);
  private projectService = inject(ProjectService);
  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private datePipe = inject(DatePipe);
  private toastrService = inject(ToastNotificationService);

  constructor() {}
  
  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.showError('Task ID not found in URL');
      this.router.navigate(['/']);
      return;
    }
    
    this.taskId = id;
    this.initForm();
    this.loadTaskData();
  }
  
  initForm(): void {
    this.taskForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', Validators.maxLength(2000)],
      startDate: [null, Validators.required],
      startTime: ['09:00'], // Default start time (9:00 AM)
      endDate: [null],
      endTime: ['17:00'], // Default end time (5:00 PM)
      status: ['TO_DO', Validators.required],
      priority: ['OPTIONAL', Validators.required]
    });
    
    // Setup date validation
    this.setupDateValidation();
  }
  
  /**
   * Load all the task data and related info.
   */
  loadTaskData(): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('User ID not found');
      this.loading = false;
      return;
    }
    
    this.taskService.getTaskById(this.taskId, userId).subscribe({
      next: (task) => {
        console.log('Task loaded:', task);
        this.task = task;
        this.projectId = task.projectId;
        
        // Now fetch project details to get allowed date ranges
        this.projectService.getProject(task.projectId).subscribe({
          next: (project: Project | undefined) => {
            if (!project) {
              console.error('Project not found');
              this.updateFormWithTaskData(task);
              this.loadAssignees(task.projectId);
              this.loadTaskDependencies(task.projectId);
              this.loadComments();
              return;
            }
            console.log('Project data loaded:', project);
            
            // Extract project dates for validation
            if (project.startDate) {
              this.projectStartDate = new Date(project.startDate);
            }
            if (project.endDate) {
              this.projectEndDate = new Date(project.endDate);
              console.log('Project end date set for validation:', this.projectEndDate);
            }
            
            // Now update the form with task data
            this.updateFormWithTaskData(task);
            
            // Load other related data in parallel
            this.loadAssignees(task.projectId);
            this.loadTaskDependencies(task.projectId);
            this.loadComments();
          },
          error: (err: any) => {
            console.error('Failed to load project:', err);
            // Still continue with task data
            this.updateFormWithTaskData(task);
            this.loadAssignees(task.projectId);
            this.loadTaskDependencies(task.projectId);
            this.loadComments();
          }
        });
      },
      error: (error) => {
        this.showError(`Failed to load task: ${error.message || 'Unknown error'}`);
        this.loading = false;
      }
    });
  }
  
  createUserMap(users: any[]): void {
    users.forEach(user => {
      this.userMap.set(user.id, user.name);
    });
  }
  
  createTaskMap(tasks: Task[]): void {
    tasks.forEach(task => {
      this.taskMap.set(task.id, task.name);
    });
  }
  
  getUserName(userId: string): string {
    return this.userMap.get(userId) || `User (${userId})`;
  }
  
  getDependencyName(taskId: string): string {
    return this.taskMap.get(taskId) || `Task (${taskId})`;
  }
  
  // This will be initialized when we load assignees to translate assignment IDs to user names
  getUserNameByAssignmentId: (assignmentId: string) => string = (assignmentId: string) => {
    return `Unknown User`;
  }
  
  /**
   * When the start date changes, automatically adjust the end date
   * to follow the same behavior as ProjectDialogComponent.
   */
  onStartDateChange(): void {
    const startDate = this.taskForm.get('startDate')?.value;
    const endDate = this.taskForm.get('endDate')?.value;
    
    // Make sure we have a start date
    if (!startDate) return;
    
    // If there's no end date or if end date is before start date, set it to start date + 1 day
    if (!endDate || endDate < startDate) {
      const newEndDate = new Date(startDate);
      newEndDate.setDate(newEndDate.getDate() + 1);
      this.taskForm.get('endDate')?.setValue(newEndDate);
      console.log('Adjusted end date to:', newEndDate);
    }
  }
  
  /**
   * Handle changes to time pickers
   */
  onTimeChange(type: 'start' | 'end'): void {
    if (type === 'start') {
      this.startTimeString = `${this.startHour}:${this.startMinute}`;
      this.taskForm.get('startTime')?.setValue(this.startTimeString);
      this.updateStartDateTime();
    } else {
      this.endTimeString = `${this.endHour}:${this.endMinute}`;
      this.taskForm.get('endTime')?.setValue(this.endTimeString);
      this.updateEndDateTime();
    }
  }
  
  /**
   * Updates the start date and time in internal model
   */
  updateStartDateTime(): void {
    // This method ensures the form time changes are applied to the task model
    const startDate = this.taskForm.get('startDate')?.value;
    if (startDate) {
      console.log('Updating start date/time with:', this.startTimeString);
    }
  }
  
  /**
   * Updates the end date and time in internal model
   */
  updateEndDateTime(): void {
    // This method ensures the form time changes are applied to the task model
    const endDate = this.taskForm.get('endDate')?.value;
    if (endDate) {
      console.log('Updating end date/time with:', this.endTimeString);
    }
  }
  
  // Legacy method kept for compatibility
  onStartTimeChange() {
    const startTime = this.taskForm.get('startTime')?.value;
    if (startTime) {
      this.startTimeString = startTime;
      const [hour, minute] = this.startTimeString.split(':');
      this.startHour = hour;
      this.startMinute = minute;
    }
  }
  
  // Legacy method kept for compatibility
  onEndTimeChange() {
    const endTime = this.taskForm.get('endTime')?.value;
    if (endTime) {
      this.endTimeString = endTime;
      const [hour, minute] = this.endTimeString.split(':');
      this.endHour = hour;
      this.endMinute = minute;
    }
  }
  
  /**
   * Filter function for the start date picker:
   * - Only allows dates on or after the original start date when editing
   * - Only allows dates on or before the project end date
   * - If no original date, allows dates from today forward
   */
  startDateFilter = (date: Date | null): boolean => {
    if (!date) return false;
    
    // Ensure we have a full loaded task with original date before filtering
    // This is important as the filter might be called before the task is loaded
    if (!this.task || this.loading) {
      // During initial loading, allow all dates to prevent blocking the UI
      // The proper validation will be applied once data is loaded
      return true;
    }
    
    // Set midnight time for proper date comparison
    const dateToCheck = new Date(date);
    dateToCheck.setHours(0, 0, 0, 0);
    
    // Check against original start date (minimum date allowed)
    let isAfterMinDate = true;
    let minDateRef = null;
    
    if (this.originalStartDate) {
      // Create a new date to avoid timezone issues
      const origStartDate = new Date(this.originalStartDate);
      origStartDate.setHours(0, 0, 0, 0);
      minDateRef = origStartDate;
      isAfterMinDate = dateToCheck >= origStartDate;
      
      // If we're filtering a date that's between original start and today,
      // log detailed information to debug
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (dateToCheck < today && dateToCheck >= origStartDate) {
        console.log('Filtering date between original and today:', {
          dateBeingChecked: dateToCheck.toISOString(),
          originalStartDate: origStartDate.toISOString(),
          result: isAfterMinDate ? 'ALLOWED' : 'BLOCKED'
        });
      }
    } else {
      // Fallback to today if no original date is available (shouldn't happen in edit mode)
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      minDateRef = today;
      isAfterMinDate = dateToCheck >= today;
    }
    
    // Check against project end date (maximum date allowed)
    let isBeforeMaxDate = true;
    if (this.projectEndDate) {
      const projEndDate = new Date(this.projectEndDate);
      projEndDate.setHours(0, 0, 0, 0);
      isBeforeMaxDate = dateToCheck <= projEndDate;
    }
    
    // Once in a while, log the state of our date filter for debugging
    if (Math.random() < 0.05) { // Log only ~5% of calls to avoid console spam
      console.log('Date filter state:', {
        dateBeingChecked: dateToCheck.toISOString(),
        minDate: minDateRef ? minDateRef.toISOString() : 'none',
        maxDate: this.projectEndDate ? this.projectEndDate.toISOString() : 'none',
        isAfterMinDate,
        isBeforeMaxDate,
        result: (isAfterMinDate && isBeforeMaxDate) ? 'ALLOWED' : 'BLOCKED'
      });
    }
    
    // Date is valid if it's both after min date and before max date
    return isAfterMinDate && isBeforeMaxDate;
  };
  
  /**
   * Filter function for the end date picker:
   * - Only allows dates on or after the selected start date
   * - Only allows dates on or before the project end date
   */
  endDateFilter = (date: Date | null): boolean => {
    if (!date) return false;
    
    // Ensure we have a fully loaded task and project data before filtering
    if (!this.task || this.loading) {
      return true; // Skip validation during initial loading
    }
    
    // Set midnight time for proper date comparison
    const dateToCheck = new Date(date);
    dateToCheck.setHours(0, 0, 0, 0);
    
    // Validate against start date (must be after start date)
    let isAfterStartDate = true;
    const startDate = this.taskForm.get('startDate')?.value;
    if (startDate) {
      const startDateCopy = new Date(startDate);
      startDateCopy.setHours(0, 0, 0, 0);
      isAfterStartDate = dateToCheck >= startDateCopy;
    }
    
    // Validate against project end date (must be before project end date)
    let isBeforeProjectEnd = true;
    if (this.projectEndDate) {
      const projectEndCopy = new Date(this.projectEndDate);
      projectEndCopy.setHours(0, 0, 0, 0);
      isBeforeProjectEnd = dateToCheck <= projectEndCopy;
      
      // Log when we're filtering out a date because it's after project end date
      if (!isBeforeProjectEnd) {
        console.log('Filtering out end date that exceeds project end date:', {
          attemptedDate: dateToCheck.toISOString(),
          projectEndDate: projectEndCopy.toISOString()
        });
      }
    }
    
    return isAfterStartDate && isBeforeProjectEnd;
  };

  
  /**
   * Setup date validation with the following rules:
   * - Task start date cannot be earlier than original start date (when editing)
   * - Start date cannot be later than project end date
   * - End date must be after or equal to start date
   * - End date cannot be later than project end date
   */
  setupDateValidation(): void {
    console.log('Setting up date validation with the following constraints:');
    console.log('- Original start date:', this.originalStartDate);
    console.log('- Original end date:', this.originalEndDate);
    console.log('- Project start date:', this.projectStartDate);
    console.log('- Project end date:', this.projectEndDate);
    
    // For editing tasks, the minimum start date is the original start date
    // This preserves the ability to keep past start dates when editing
    if (this.originalStartDate) {
      this.minStartDate = this.originalStartDate;
      console.log('Using original start date as the minimum date');
    } else {
      // If no original start date (shouldn't happen in edit mode), use today
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      this.minStartDate = today;
      console.log('No original start date found, using today as fallback');
    }
    
    console.log('Min start date set to:', this.minStartDate ? this.minStartDate.toISOString() : 'not set');
    
    // Add a listener to the startDate control for both validation and to adjust endDate
    this.taskForm.get('startDate')?.valueChanges.subscribe(value => {
      if (value) {
        // Trigger the start date change handler to adjust end date if needed
        this.onStartDateChange();
        
        const selectedDate = new Date(value);
        let isValid = true;
        let errorMessage = '';
        
        // Validate against original start date (can't be earlier)
        if (this.originalStartDate && selectedDate < this.originalStartDate) {
          isValid = false;
          errorMessage = 'Data rozpoczęcia nie może być wcześniejsza niż oryginalna data rozpoczęcia zadania';
        }
        
        // Validate against project end date (can't be later)
        if (isValid && this.projectEndDate && selectedDate > this.projectEndDate) {
          isValid = false;
          errorMessage = 'Data rozpoczęcia nie może być późniejsza niż data zakończenia projektu';
        }
        
        // If validation failed, reset to appropriate date and show error
        if (!isValid) {
          // Reset to original date if that was the issue
          if (errorMessage.includes('oryginalna')) {
            this.taskForm.get('startDate')?.setValue(this.originalStartDate);
          } else if (this.projectEndDate) {
            // Reset to project end date if that was the issue
            this.taskForm.get('startDate')?.setValue(this.projectEndDate);
          }
          this.showError(errorMessage);
        }
      }
    });
    
    // Add a listener for endDate changes to ensure proper validation
    this.taskForm.get('endDate')?.valueChanges.subscribe(value => {
      if (value) {
        const startDate = this.taskForm.get('startDate')?.value;
        const endDate = new Date(value);
        let isValid = true;
        let errorMessage = '';
        
        // Validate that end date is not before start date
        if (startDate && endDate < startDate) {
          isValid = false;
          errorMessage = 'Data zakończenia nie może być wcześniejsza niż data rozpoczęcia';
        }
        
        // Validate that end date is not after project end date
        if (isValid && this.projectEndDate && endDate > this.projectEndDate) {
          isValid = false;
          errorMessage = 'Data zakończenia nie może być późniejsza niż data zakończenia projektu';
        }
        
        // If validation failed, reset to appropriate date and show error
        if (!isValid) {
          if (errorMessage.includes('rozpoczęcia')) {
            // End date before start date, set to start date + 1 day
            const correctedEndDate = new Date(startDate);
            correctedEndDate.setDate(correctedEndDate.getDate() + 1);
            this.taskForm.get('endDate')?.setValue(correctedEndDate);
          } else if (this.projectEndDate) {
            // End date after project end, set to project end date
            this.taskForm.get('endDate')?.setValue(this.projectEndDate);
          }
          this.showError(errorMessage);
        }
      }
    });
  }
  
  /**
   * Validates dates with the following rules:
   * 1. Start date is not earlier than the original start date (when editing)
   * 2. Start date is not later than project end date
   * 3. End date is after or equal to start date
   * 4. End date is not later than project end date
   * @returns true if date validation passes, false otherwise
   */
  isValidDateRange(): boolean {
    const startDate = this.taskForm.get('startDate')?.value;
    
    if (!startDate) {
      return false; // Start date must be set
    }
    
    console.log('Validating dates with constraints:', {
      startDate: startDate,
      originalStartDate: this.originalStartDate,
      projectEndDate: this.projectEndDate
    });
    
    // 1. Validate against original start date (can't be earlier)
    if (this.originalStartDate && startDate < this.originalStartDate) {
      this.showError('Data rozpoczęcia nie może być wcześniejsza niż oryginalna data rozpoczęcia zadania');
      return false;
    }
    
    // 2. Validate against project end date (can't be later)
    if (this.projectEndDate && startDate > this.projectEndDate) {
      this.showError('Data rozpoczęcia nie może być późniejsza niż data zakończenia projektu');
      return false;
    }
    
    // 3. Validate end date is after start date
    const endDate = this.taskForm.get('endDate')?.value;
    if (endDate && endDate < startDate) {
      this.showError('Data zakończenia nie może być wcześniejsza niż data rozpoczęcia');
      return false;
    }
    
    // 4. Validate end date is not after project end date
    if (endDate && this.projectEndDate && endDate > this.projectEndDate) {
      this.showError('Data zakończenia nie może być późniejsza niż data zakończenia projektu');
      return false;
    }
    
    // All validations passed
    return true;
  }
  
  /**
   * Returns a formatted date string for display in the error message
   * Always returns the original task start date for editing
   */
  getMinDateLabel(): string {
    if (!this.originalStartDate) {
      return 'oryginalną datą rozpoczęcia';
    }
    
    // Always return the original task start date
    return `oryginalną datą rozpoczęcia zadania (${this.datePipe.transform(this.originalStartDate, 'yyyy-MM-dd')})`;
  }

  /**
   * Formats a date to the yyyy-MM-dd format required by the backend
   * Uses direct date component extraction to avoid timezone issues (like in project-dialog)
   */
  formatDate(date: Date): string | null {
    if (!date) return null;
    // Use direct date component extraction to avoid timezone issues
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

  /**
   * Method stub for error handling - no longer shows toast messages directly
   */
  showError(message: string): void {
    // Error will be handled in the UI without toast notification
    console.error(message);
  }

  /**
   * Method stub for success handling - no longer shows toast messages directly
   */
  showSuccess(message: string): void {
    // Success will be handled in the UI without toast notification
    console.log(message);
  }

  /**
   * Refreshes task data from the server to ensure we have the latest state
   * Used after operations that may have succeeded but returned serialization errors
   */
  refreshTaskData(): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      console.error('Cannot refresh task data: User ID is missing');
      return;
    }

    console.log('Refreshing task data to get latest state...');
    this.taskService.getTaskById(this.taskId, userId).subscribe({
      next: (refreshedTask) => {
        console.log('Task data refreshed successfully:', refreshedTask);
        this.task = refreshedTask;
      },
      error: (err) => {
        console.error('Error refreshing task data:', err);
      }
    });
  }

  /**
   * Checks if the form is valid for submission
   * This includes both Angular's built-in validation and our custom date validation
   */
  isFormValid(): boolean {
    // First check if any controls are invalid according to Angular's validation
    if (!this.taskForm.valid) {
      // Log the specific validation issues for debugging
      const controls = this.taskForm.controls;
      for (const name in controls) {
        if (controls[name].invalid) {
          console.log(`Control '${name}' is invalid:`, controls[name].errors);
        }
      }
      return false;
    }

    // Additional custom validation for date range
    if (!this.isValidDateRange()) {
      return false;
    }

    return true;
  }

  /**
   * Submit form for task editing
   */
  onSubmit(): void {
    console.log('Form submission attempt');

    // Double-check form validity before submission
    if (!this.isFormValid()) {
      console.log('Form validation failed at submission time');
      this.showError('Formularz zawiera błędy. Proszę poprawić dane.');
      return;
    }

    // Log validation status to help with debugging
    console.log('Form validation passed. Proceeding with submission.');

    this.submitting = true;
    const userId = this.authService.getUserId();
    if (!userId || !this.taskId) {
      this.showError('User ID is missing or task ID is not loaded.');
      this.submitting = false;
      return;
    }

    // Get form values
    const formValue = this.taskForm.value;
    
    // Get time values
    const startTime = formValue.startTime || '09:00';
    const endTime = formValue.endTime || '17:00';
    
    // Create formatted ISO string dates with time components
    let formattedStartDate = null;
    let formattedEndDate = null;
    
    if (formValue.startDate) {
      // Format date with time: YYYY-MM-DDThh:mm:00
      const startDate = new Date(formValue.startDate);
      const [hours, minutes] = startTime.split(':').map(Number);
      formattedStartDate = `${this.formatDate(startDate)}T${startTime}:00`;
      console.log('Formatted start date with time:', formattedStartDate);
    }
    
    if (formValue.endDate) {
      // Format date with time: YYYY-MM-DDThh:mm:00
      const endDate = new Date(formValue.endDate);
      formattedEndDate = `${this.formatDate(endDate)}T${endTime}:00`;
      console.log('Formatted end date with time:', formattedEndDate);
    }
    
    // Debugging to verify correct date formatting
    console.log('Original startDate from form:', formValue.startDate);
    console.log('Original startTime from form:', formValue.startTime);
    console.log('Original endDate from form:', formValue.endDate);
    console.log('Original endTime from form:', formValue.endTime);

    // Create the task update object from form values
    const taskUpdate: TaskUpdate = {
      id: this.taskId,
      name: formValue.name,
      description: formValue.description,
      startDate: formattedStartDate as string | undefined,
      endDate: formattedEndDate as string | undefined,
      status: formValue.status,
      priority: formValue.priority,
      // Use task's current values for these complex properties
      assigneeIds: this.task?.assigneeIds || [],
      comments: this.task?.comments || [],
      files: this.task?.files || [],
      // Keep project and dependencies
      projectId: this.task?.projectId || '',
      dependencyIds: this.task?.dependencyIds || []
    };

    console.log('Formatted task update:', taskUpdate);

    this.taskService.updateTask(this.taskId, taskUpdate, userId).subscribe({
      next: (updatedTask) => {
        console.log('Task updated successfully:', updatedTask);
        this.task = updatedTask;
        this.submitting = false;
        this.showSuccess('Zadanie zostało zaktualizowane.');
        this.router.navigate(['/projects', this.projectId]);
      },
      error: (err: any) => {
        console.error('Error updating task:', err);
        this.submitting = false;
        this.showError(`Nie udało się zaktualizować zadania: ${err.message || 'Nieznany błąd'}`);
      }
    });
  }
  
  addAssignee(): void {
    if (!this.selectedAssigneeId) return;
    
    this.submitting = true;
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('User ID is missing.');
      this.submitting = false;
      return;
    }
    
    // Get the selected user information from available users
    const selectedUser = this.availableUsers.find(user => user.id === this.selectedAssigneeId);
    if (!selectedUser) {
      this.showError('Selected user not found in available users.');
      this.submitting = false;
      return;
    }

    /**
     * First update the task locally to avoid UI delay
     */
    const optimisticUpdate = (assigneeObject?: TaskAssignee) => {
      // Update the UI optimistically, before the server responds
      if (!this.task!.assigneeIds) {
        this.task!.assigneeIds = [];
      }
      
      // Only add if not already in the list
      let assignmentId: string;
      
      if (assigneeObject) {
        // If we have the actual assignee object from API response
        assignmentId = assigneeObject.id;
        
        // Update our mapping immediately so the user name shows correctly
        const oldFn = this.getUserNameByAssignmentId;
        this.getUserNameByAssignmentId = (id: string): string => {
          if (id === assignmentId) {
            // For the newly added user, use their name from availableUsers
            return selectedUser.name;
          }
          // For existing users, use the previous function
          return oldFn(id);
        };
      } else {
        // If we're doing optimistic update before API response (will be temporary)
        // Create a temporary ID - will be replaced after API response
        assignmentId = `temp-${Date.now()}`;
        
        // Update our mapping with temporary ID
        const oldFn = this.getUserNameByAssignmentId;
        this.getUserNameByAssignmentId = (id: string): string => {
          if (id === assignmentId) {
            // For the newly added user, use their name from availableUsers
            return selectedUser.name;
          }
          // For existing users, use the previous function
          return oldFn(id);
        };
      }
      
      // Add the assignment to the task
      if (!this.task!.assigneeIds.includes(assignmentId)) {
        this.task!.assigneeIds.push(assignmentId);
      }
      
      this.selectedAssigneeId = null;
    };

    this.taskService.addAssignee(this.taskId, this.selectedAssigneeId, userId).subscribe({
      next: (assignee) => {
        console.log('Successfully added assignee:', assignee);
        // Update with the actual assignee object from the API
        optimisticUpdate(assignee);
        this.submitting = false;
        this.showSuccess('Użytkownik został dodany');
        
        // Update our userMap for future reference
        this.userMap.set(this.selectedAssigneeId!, selectedUser.name);
        
        // Refresh task data to ensure we have the latest state from server
        this.refreshTaskData();
      },
      error: (err) => {
        console.error('Error adding assignee:', err);
        this.submitting = false;
        
        // Check if it's a serialization error with LocalDate
        if (err.error && typeof err.error === 'string' && 
            (err.error.includes('java.time.LocalDate') || 
             err.error.includes('jackson-datatype-jsr310'))) {
          // Special handling for date serialization errors
          console.log('LocalDate serialization error detected. Assuming assignee was added successfully.');
          // Proceed with optimistic update without assignee object (we'll rely on refresh)
          optimisticUpdate();
          this.showSuccess('Użytkownik prawdopodobnie został dodany. Odświeżanie danych...');
          // Refresh task data to get the current state
          this.refreshTaskData();
        } else {
          // Standard error handling for other errors
          this.showError(`Nie udało się dodać użytkownika: ${err.status} ${err.statusText}. 
            Upewnij się, że użytkownik jest członkiem projektu.`);
        }
      }
    });
  }

  removeAssignee(assigneeId: string): void {
    this.submitting = true;
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('User ID is missing.');
      this.submitting = false;
      return;
    }
    
    this.taskService.removeAssignee(this.taskId, assigneeId, userId).subscribe({
      next: () => {
        console.log('Successfully removed assignee');
        // Update the task's assignees list
        if (this.task?.assigneeIds) {
          this.task.assigneeIds = this.task.assigneeIds.filter(id => id !== assigneeId);
        }
        this.submitting = false;
        this.showSuccess('Użytkownik został usunięty');
      },
      error: (err) => {
        console.error('Error removing assignee:', err);
        this.submitting = false;
        
        // Check if it's a serialization error with LocalDate
        if (err.error && typeof err.error === 'string' && 
            (err.error.includes('java.time.LocalDate') || 
             err.error.includes('jackson-datatype-jsr310'))) {
          // Special handling for date serialization errors
          console.log('LocalDate serialization error detected. Assuming assignee was removed successfully.');
          // Remove the assignee locally
          if (this.task?.assigneeIds) {
            this.task.assigneeIds = this.task.assigneeIds.filter(id => id !== assigneeId);
          }
          this.showSuccess('Użytkownik prawdopodobnie został usunięty. Odświeżanie danych...');
          // Refresh task data to get the current state
          this.refreshTaskData();
        } else {
          this.showError(`Nie udało się usunąć użytkownika: ${err.message || 'Nieznany błąd'}`);
        }
      }
    });
  }
  
  addDependency(): void {
    if (!this.selectedDependencyId) return;
    
    this.submitting = true;
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('User ID is missing.');
      this.submitting = false;
      return;
    }
    
    // Prevent self-dependency
    if (this.selectedDependencyId === this.taskId) {
      this.showError('Zadanie nie może zależeć od samego siebie.');
      this.submitting = false;
      return;
    }
    
    this.taskService.addDependency(this.taskId, this.selectedDependencyId, userId).subscribe({
      next: () => {
        console.log('Successfully added dependency');
        // Update the task's dependencies list
        if (!this.task!.dependencyIds) {
          this.task!.dependencyIds = [];
        }
        this.task!.dependencyIds.push(this.selectedDependencyId!);
        
        this.selectedDependencyId = null;
        this.submitting = false;
        this.showSuccess('Zależność została dodana');
      },
      error: (err) => {
        console.error('Error adding dependency:', err);
        this.submitting = false;
        this.showError(`Nie udało się dodać zależności: ${err.message || 'Nieznany błąd'}`);
      }
    });
  }
  
  removeDependency(dependencyId: string): void {
    this.submitting = true;
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('User ID is missing.');
      this.submitting = false;
      return;
    }
    
    this.taskService.removeDependency(this.taskId, dependencyId, userId).subscribe({
      next: () => {
        console.log('Successfully removed dependency');
        // Update the task's dependencies list
        if (this.task?.dependencyIds) {
          this.task.dependencyIds = this.task.dependencyIds.filter(id => id !== dependencyId);
        }
        this.submitting = false;
        this.showSuccess('Zależność została usunięta');
      },
      error: (err) => {
        console.error('Error removing dependency:', err);
        this.submitting = false;
        this.showError(`Nie udało się usunąć zależności: ${err.message || 'Nieznany błąd'}`);
      }
    });
  }
  
  /**
   * Add a comment to the task
   */
  addComment(): void {
    if (!this.task || !this.newComment.trim()) {
      return;
    }

    this.submitting = true;
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('Nie udało się zidentyfikować użytkownika');
      this.submitting = false;
      return;
    }

    this.taskService.addComment(this.taskId, this.newComment, userId).subscribe({
      next: (comment) => {
        console.log('Successfully added comment:', comment);
        // Update the task's comments list
        if (!this.task!.comments) {
          this.task!.comments = [];
        }
        this.task!.comments.push(comment);
        this.newComment = ''; // Clear the input
        this.submitting = false;
        this.showSuccess('Komentarz został dodany');
      },
      error: (err) => {
        console.error('Error adding comment:', err);
        this.submitting = false;
        this.showError(`Nie udało się dodać komentarza: ${err.message || 'Nieznany błąd'}`);
      }
    });
  }

  /**
   * Delete a comment by its ID
   * @param commentId The ID of the comment to delete
   */
  deleteComment(commentId: string): void {
    if (!commentId || !this.taskId) {
      return;
    }

    if (!confirm('Czy na pewno chcesz usunąć ten komentarz? Tej operacji nie można cofnąć.')) {
      return;
    }

    this.submitting = true;
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('Nie udało się zidentyfikować użytkownika');
      this.submitting = false;
      return;
    }

    this.taskService.deleteComment(this.taskId, commentId, userId).subscribe({
      next: () => {
        console.log('Successfully deleted comment:', commentId);
        // Remove the comment from the task's comments list
        if (this.task?.comments) {
          this.task.comments = this.task.comments.filter(comment => comment.id !== commentId);
        }
        this.submitting = false;
        this.showSuccess('Komentarz został usunięty');
      },
      error: (err) => {
        console.error('Error deleting comment:', err);
        this.submitting = false;
        this.showError(`Nie udało się usunąć komentarza: ${err.message || 'Nieznany błąd'}`);
      }
    });
  }

  /**
   * Confirms and deletes all comments for the current task
   */
  confirmDeleteAllComments(): void {
    if (!this.taskId) {
      return;
    }

    if (!confirm('Czy na pewno chcesz usunąć WSZYSTKIE komentarze? Tej operacji nie można cofnąć.')) {
      return;
    }

    this.submitting = true;
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('Nie udało się zidentyfikować użytkownika');
      this.submitting = false;
      return;
    }

    this.taskService.deleteAllCommentsInTask(this.taskId, userId).subscribe({
      next: () => {
        console.log('Successfully deleted all comments');
        // Clear all comments from the task
        if (this.task) {
          this.task.comments = [];
        }
        this.submitting = false;
        this.showSuccess('Wszystkie komentarze zostały usunięte');
      },
      error: (err) => {
        console.error('Error deleting all comments:', err);
        this.submitting = false;
        this.showError(`Nie udało się usunąć komentarzy: ${err.message || 'Nieznany błąd'}`);
      }
    });
  }

  /**
   * Load assignees and available project members.
   */
  loadAssignees(projectId: string): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('User ID not found');
      return;
    }
    
    // First, load all the project members for name lookup
    this.projectService.getProjectMembers(projectId).subscribe({
      next: (members) => {
        this.availableUsers = (Array.isArray(members) ? members : Object.values(members)).map((member: ProjectMemberDTO) => ({
          id: member.userId,
          name: member.name
        }));
        
        // Create a map of user IDs to names for lookup
        this.createUserMap(this.availableUsers);
        
        // Now fetch the actual task assignees to get the correct user IDs
        if (this.task && this.task.assigneeIds && this.task.assigneeIds.length > 0) {
          console.log('Getting detailed assignee information for task...');
          
          // Get the complete assignee objects with taskId, id (assignment id), and userId (the one we need)
          this.taskService.getTaskAssignees(this.taskId, userId).subscribe({
            next: (assignees) => {
              if (assignees && assignees.length > 0) {
                console.log('Received task assignees:', assignees);
                
                // Clear task assignee IDs (they're assignment IDs, not user IDs)
                if (this.task) {
                  // Store assignment IDs for operations like removal
                  const assignmentIds = [...(this.task.assigneeIds || [])];
                  
                  // Create a mapping from assignment ID to user ID
                  const assignmentToUserIdMap = new Map<string, string>();
                  assignees.forEach(assignee => {
                    assignmentToUserIdMap.set(assignee.id, assignee.userId);
                  });
                  
                  // Create a getUserNameByAssignmentId function that first translates assignment ID to user ID
                  // and then looks up the user name
                  this.getUserNameByAssignmentId = (assignmentId: string): string => {
                    const actualUserId = assignmentToUserIdMap.get(assignmentId);
                    if (actualUserId) {
                      return this.userMap.get(actualUserId) || `User (${actualUserId})`;
                    }
                    return `Unknown User`;
                  }
                }
                
                // For each assignee, check if we have their user name in our map
                assignees.forEach(assignee => {
                  const userId = assignee.userId;
                  if (!this.userMap.has(userId)) {
                    // Try to find the user in available users
                    const user = this.availableUsers.find(u => u.id === userId);
                    if (user) {
                      this.userMap.set(userId, user.name);
                      console.log(`Added assignee from project members: ${user.name} (${userId})`);
                    } else {
                      // No name found, use a default label
                      this.userMap.set(userId, `User ${userId}`);
                      console.log(`No name found for user ${userId}, using default label`);
                    }
                  }
                });
              }
            },
            error: (assigneeErr) => {
              console.error('Failed to load task assignees:', assigneeErr);
            }
          });
        }
        
        // Complete loading
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load project members:', err);
        this.showError(`Nie udało się załadować członków projektu: ${err.message || 'Nieznany błąd'}`);
        this.loading = false;
      }
    });
  }
  
  /**
   * Loads all tasks from the project that can be set as dependencies.
   */
  loadTaskDependencies(projectId: string): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('User ID not found');
      return;
    }
    
    this.taskService.getTasksByProject(projectId, userId).subscribe({
      next: (tasks) => {
        // Filter out the current task (can't depend on itself)
        this.availableTasks = tasks.filter(task => task.id !== this.taskId);
        
        // Create a map of task IDs to names for lookup
        this.createTaskMap(this.availableTasks);
      },
      error: (err) => {
        console.error('Failed to load tasks for dependencies:', err);
        this.showError(`Nie udało się załadować zadań: ${err.message || 'Nieznany błąd'}`);
      }
    });
  }
  
  /**
   * Load comments if not already included with the task.
   */
  loadComments(): void {
    // In this implementation, comments are loaded with the task
    // This is a placeholder in case we need separate loading in future
  }
  
  /**
   * Update the form with values from the loaded task.
   */
  
  // Populate the form with task data and extract time components
  updateFormWithTaskData(task: Task): void {
    console.log('Updating form with task data:', task);
    
    // Important: Store the task reference first, used by our filter
    this.task = task;
    
    // Extract date and time components from task dates
    let startDate = null;
    let startTime = '09:00'; // Default start time
    let endDate = null;
    let endTime = '17:00';   // Default end time
    
    // Process start date and time if available
    if (task.startDate) {
      const dateTimeStr = task.startDate;
      startDate = new Date(dateTimeStr);
      
      // Store original start date for validation - CRITICAL FOR CORRECT FILTERING
      this.originalStartDate = new Date(dateTimeStr);
      console.log('Set original start date for validation:', this.originalStartDate);
      console.log('Original date in ISO format:', this.originalStartDate.toISOString());
      console.log('Today is:', new Date().toISOString());
      
      // Extract time component if available (ISO format has a 'T' separator)
      if (dateTimeStr.includes('T')) {
        const timePart = dateTimeStr.split('T')[1];
        if (timePart) {
          startTime = timePart.substring(0, 5); // Get HH:MM part
          // Update the time components for the new UI
          const [hour, minute] = startTime.split(':');
          this.startHour = hour;
          this.startMinute = minute;
          this.startTimeString = startTime;
        }
      }
    }
    
    // Process end date and time if available
    if (task.endDate) {
      const dateTimeStr = task.endDate;
      endDate = new Date(dateTimeStr);
      
      // Store original end date for validation
      this.originalEndDate = new Date(dateTimeStr);
      
      // Extract time component if available
      if (dateTimeStr.includes('T')) {
        const timePart = dateTimeStr.split('T')[1];
        if (timePart) {
          endTime = timePart.substring(0, 5); // Get HH:MM part
          // Update the time components for the new UI
          const [hour, minute] = endTime.split(':');
          this.endHour = hour;
          this.endMinute = minute;
          this.endTimeString = endTime;
        }
      }
    }
    
    console.log('Extracted date-time values:', {
      startDate: startDate,
      startTime: startTime,
      endDate: endDate,
      endTime: endTime,
      startHour: this.startHour,
      startMinute: this.startMinute,
      endHour: this.endHour,
      endMinute: this.endMinute
    });
    
    // Update the form with all values including date and time
    this.taskForm.patchValue({
      name: task.name,
      description: task.description,
      startDate: startDate,
      startTime: startTime,
      endDate: endDate,
      endTime: endTime,
      status: task.status,
      priority: task.priority
    });
    
    // Make sure time values are synchronized between form and component properties
    this.startTimeString = startTime;
    this.endTimeString = endTime;
    
    this.loading = false;
  }
  /**
   * Cancels editing and returns to the project view.
   */
  cancel(): void {
    this.router.navigate(['/projects', this.projectId]);
  }
}
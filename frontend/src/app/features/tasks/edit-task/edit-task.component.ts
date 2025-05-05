import { Component, OnInit, inject } from '@angular/core';
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
import { Task, TaskUpdate, TaskAssignee, TaskComment } from '../../../shared/models/task.model';
import { TaskPriority } from '../../../shared/enums/task-priority.enum';
import { TaskStatus } from '../../../shared/enums/task-status.enum';
import { AuthService } from '../../../shared/services/auth.service';
import { ProjectService } from '../../../shared/services/project.service';
import { ProjectMemberDTO, Project } from '../../../shared/models/project.model';
import { Observable, forkJoin, catchError, throwError, switchMap } from 'rxjs';

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
  
  // Date handling for validation
  projectStartDate: Date | null = null;
  originalStartDate: Date | null = null;
  minStartDate: Date | null = null;
  
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
      endDate: [null],
      status: ['TO_DO', Validators.required],
      priority: ['OPTIONAL', Validators.required]
    });
    
    // Setup date validation
    this.setupDateValidation();
  }
  
  // Load all the task data and related info
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
            
            // Extract project start date for validation
            if (project.startDate) {
              this.projectStartDate = new Date(project.startDate);
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
  
  /**
   * When the start date changes, automatically adjust the end date
   * to follow the same behavior as ProjectDialogComponent
   */
  onStartDateChange(): void {
    const startDate = this.taskForm.get('startDate')?.value;
    const endDate = this.taskForm.get('endDate')?.value;
    
    // Make sure we have a start date
    if (!startDate) return;
    
    // If there's no end date or if end date is before start date, set it to start date + 1 day
    if (!endDate || new Date(endDate) < new Date(startDate)) {
      const newEndDate = new Date(startDate);
      newEndDate.setDate(newEndDate.getDate() + 1);
      this.taskForm.get('endDate')?.setValue(newEndDate);
      console.log('Adjusted end date to:', newEndDate);
    }
  }
  
  /**
   * Setup date validation with the following rules:
   * - Task start date cannot be earlier than project start date
   * - Task start date cannot be earlier than today
   * - End date must be after or equal to start date
   */
  setupDateValidation(): void {
    // Initialize minimum date to today (for new tasks)
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    // Determine the minimum start date based on project start date and today
    if (this.projectStartDate) {
      const projectStartCopy = new Date(this.projectStartDate);
      projectStartCopy.setHours(0, 0, 0, 0);
      
      // Use the later of today or project start date as the minimum date
      this.minStartDate = projectStartCopy > today ? projectStartCopy : today;
    } else {
      this.minStartDate = today;
    }
    
    console.log('Min start date set to:', this.minStartDate ? this.minStartDate.toISOString() : 'not set');
    
    // Add a listener to the startDate control for both validation and to adjust endDate
    this.taskForm.get('startDate')?.valueChanges.subscribe(value => {
      if (value) {
        // Trigger the start date change handler to adjust end date if needed
        this.onStartDateChange();
        
        const selectedDate = new Date(value);
        
        // If the selected date is before the minimum start date
        if (this.minStartDate && selectedDate < this.minStartDate) {
          // Reset to the minimum start date
          this.taskForm.get('startDate')?.setValue(this.minStartDate);
          
          // Show appropriate error message
          if (this.projectStartDate && this.minStartDate && this.minStartDate.getTime() === this.projectStartDate.getTime()) {
            this.showError('Zadanie nie może rozpocząć się przed datą rozpoczęcia projektu');
          } else {
            this.showError('Zadanie nie może rozpocząć się przed dzisiejszą datą');
          }
        }
      }
    });
  }
  
  /**
   * Validates that:
   * 1. Start date is not earlier than project start date
   * 2. Start date is not earlier than today
   * 3. End date is after or equal to start date
   * @returns true if date validation passes, false otherwise
   */
  isValidDateRange(): boolean {
    const startDate = this.taskForm.get('startDate')?.value;
    
    if (!startDate) {
      return false; // Start date must be set
    }
    
    const startDateObj = new Date(startDate);
    
    // Validate against the minimum start date (which is already the max of project start and today)
    if (this.minStartDate && startDateObj < this.minStartDate) {
      if (this.projectStartDate && this.minStartDate && this.minStartDate.getTime() === this.projectStartDate.getTime()) {
        this.showError('Zadanie nie może rozpocząć się przed datą rozpoczęcia projektu');
      } else {
        this.showError('Zadanie nie może rozpocząć się przed dzisiejszą datą');
      }
      return false;
    }
    
    // Validate end date is after start date
    const endDate = this.taskForm.get('endDate')?.value;
    if (endDate && new Date(endDate) < startDateObj) {
      this.showError('Data zakończenia nie może być wcześniejsza niż data rozpoczęcia');
      return false;
    }
    
    return true;
  }
  
  /**
   * Returns a formatted date string for display in the error message
   * Displays either project start date or today, whichever is later
   */
  getMinDateLabel(): string {
    if (!this.minStartDate) {
      return 'dziś';
    }
    
    // Format the date as DD.MM.YYYY (Polish format)
    return this.datePipe.transform(this.minStartDate, 'dd.MM.yyyy') || 'dziś';
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
    
    // Then perform our custom date validation
    const isDateRangeValid = this.isValidDateRange();
    console.log('Custom date validation result:', isDateRangeValid);
    return isDateRangeValid;
  }

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
    
    // Format dates using the DatePipe approach like in ProjectService
    const formattedStartDate = formValue.startDate ? this.formatDate(formValue.startDate) : null;
    const formattedEndDate = formValue.endDate ? this.formatDate(formValue.endDate) : null;
    
    // Debugging to verify correct date formatting
    console.log('Original startDate from form:', formValue.startDate);
    console.log('Formatted startDate for API:', formattedStartDate);
    console.log('Original endDate from form:', formValue.endDate);
    console.log('Formatted endDate for API:', formattedEndDate);
    
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
    
    // Add more debugging info
    console.log('Adding assignee - Task ID:', this.taskId);
    console.log('User ID to add:', this.selectedAssigneeId);
    console.log('Current user ID:', userId);
    console.log('Project ID:', this.projectId);
    
    // First update the task locally to avoid UI delay
    const optimisticUpdate = () => {
      // Update the UI optimistically, before the server responds
      if (!this.task!.assigneeIds) {
        this.task!.assigneeIds = [];
      }
      // Only add if not already in the list
      if (!this.task!.assigneeIds.includes(this.selectedAssigneeId!)) {
        this.task!.assigneeIds.push(this.selectedAssigneeId!);
      }
      this.selectedAssigneeId = null;
    };

    this.taskService.addAssignee(this.taskId, this.selectedAssigneeId, userId).subscribe({
      next: (assignee) => {
        console.log('Successfully added assignee:', assignee);
        // Update already done optimistically
        optimisticUpdate();
        this.submitting = false;
        this.showSuccess('Użytkownik został dodany');
        
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
          // Proceed with optimistic update
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
  
  addComment(): void {
    if (!this.newComment) return;
    
    this.submitting = true;
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('User ID is missing.');
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
        
        this.newComment = '';
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
   * Formats a date to the yyyy-MM-dd format required by the backend
   */
  formatDate(date: Date): string | null {
    if (!date) return null;
    return this.datePipe.transform(date, 'yyyy-MM-dd') || null;
  }
  
  /**
   * Loads project members who can be assigned to the task
   */
  loadAssignees(projectId: string): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      this.showError('User ID not found');
      return;
    }
    
    this.projectService.getProjectMembers(projectId).subscribe({
      next: (members) => {
        this.availableUsers = (Array.isArray(members) ? members : Object.values(members)).map((member: ProjectMemberDTO) => ({
          id: member.userId,
          name: member.name
        }));
        
        // Create a map of user IDs to names for lookup
        this.createUserMap(this.availableUsers);
        
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
   * Loads all tasks from the project that can be set as dependencies
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
   * Load comments if not already included with the task
   */
  loadComments(): void {
    // In this implementation, comments are loaded with the task
    // This is a placeholder in case we need separate loading in future
  }
  
  /**
   * Update the form with values from the loaded task
   */
  updateFormWithTaskData(task: Task): void {
    // Convert date strings to Date objects
    const startDate = task.startDate ? new Date(task.startDate) : null;
    const endDate = task.endDate ? new Date(task.endDate) : null;
    
    // Save original start date for validation
    this.originalStartDate = startDate;
    
    // Update form values
    this.taskForm.patchValue({
      name: task.name,
      description: task.description,
      startDate: startDate,
      endDate: endDate,
      status: task.status,
      priority: task.priority
    });
    
    console.log('Form updated with task data', this.taskForm.value);
  }
  
  /**
   * Helper methods for showing success and error messages
   */
  showSuccess(message: string): void {
    this.snackBar.open(message, 'OK', {
      duration: 3000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
      panelClass: ['success-snackbar']
    });
  }
  
  showError(message: string): void {
    this.snackBar.open(message, 'OK', {
      duration: 5000,
      horizontalPosition: 'center',
      verticalPosition: 'bottom',
      panelClass: ['error-snackbar']
    });
  }
  
  /**
   * Checks if the current user is an assignee for this task
   */
  isUserAssigned(): boolean {
    const userId = this.authService.getUserId();
    return !!userId && !!this.task?.assigneeIds?.includes(userId);
  }
  
  /**
   * Cancels editing and returns to the project view
   */
  cancel(): void {
    this.router.navigate(['/projects', this.projectId]);
  }
}
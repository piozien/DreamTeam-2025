import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TaskService } from '../../../shared/services/task.service';
import { AuthService } from '../../../shared/services/auth.service';
import { ProjectService } from '../../../shared/services/project.service';
import { Task } from '../../../shared/models/task.model';
import { Project } from '../../../shared/models/project.model';
import { TaskPriority } from '../../../shared/enums/task-priority.enum';
import { TaskStatus } from '../../../shared/enums/task-status.enum';
import { ToastrService } from 'ngx-toastr';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

interface TasksByProject {
  project: Project;
  tasks: Task[];
}

@Component({
  selector: 'app-tasks-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDividerModule,
    MatChipsModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './tasks-page.component.html',
  styleUrls: ['./tasks-page.component.scss']
})
export class TasksPageComponent implements OnInit {
  loading = true;
  tasksByProject: TasksByProject[] = [];
  
  // For easy access to enums in the template
  TaskStatus = TaskStatus;
  TaskPriority = TaskPriority;
  
  // Service injections
  private taskService = inject(TaskService);
  private authService = inject(AuthService);
  private projectService = inject(ProjectService);
  private toastr = inject(ToastrService);
  
  ngOnInit(): void {
    this.loadUserTasks();
  }
  
  /**
   * Loads all tasks assigned to the current user and groups them by project
   */
  loadUserTasks(): void {
    const userId = this.authService.getUserId();
    if (!userId) {
      this.toastr.error('User ID not found');
      this.loading = false;
      return;
    }
    
    // First get all the tasks assigned to the user
    this.taskService.getUserTasks(userId).pipe(
      catchError(error => {
        console.error('Error loading user tasks:', error);
        this.toastr.error('Nie udało się załadować zadań');
        return of([]);
      }),
      switchMap(tasks => {
        if (tasks.length === 0) {
          return of([]);
        }
        
        // Extract unique project IDs from tasks
        const projectIds = [...new Set(tasks.map(task => task.projectId))];
        
        // Create an array of project detail observables
        const projectObservables = projectIds.map(projectId => 
          this.projectService.getProject(projectId).pipe(
            catchError(() => of(null))
          )
        );
        
        // Return tasks and their projects
        return forkJoin(projectObservables).pipe(
          map(projects => {
            // Filter out null projects
            const validProjects = projects.filter(project => project !== null) as Project[];
            
            // Group tasks by project
            return validProjects.map(project => {
              const projectTasks = tasks.filter(task => task.projectId === project.id);
              return { project, tasks: projectTasks } as TasksByProject;
            });
          })
        );
      })
    ).subscribe({
      next: (result) => {
        this.tasksByProject = result;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error processing tasks and projects:', error);
        this.toastr.error('Wystąpił błąd podczas ładowania zadań');
        this.loading = false;
      }
    });
  }
  
  /**
   * Returns the priority class for styling based on task priority
   * @param priority The task priority or undefined
   * @returns CSS class name for the priority
   */
  getPriorityClass(priority: TaskPriority | undefined): string {
    if (priority === undefined) {
      return 'priority-optional'; // Default fallback
    }
    
    switch (priority) {
      case TaskPriority.CRITICAL:
        return 'priority-critical';
      case TaskPriority.IMPORTANT:
        return 'priority-important';
      case TaskPriority.OPTIONAL:
      default:
        return 'priority-optional';
    }
  }
  
  /**
   * Returns the status class for styling based on task status
   * @param status The task status or undefined
   * @returns CSS class name for the status
   */
  getStatusClass(status: TaskStatus | undefined): string {
    if (status === undefined) {
      return 'status-todo'; // Default fallback
    }
    
    switch (status) {
      case TaskStatus.TO_DO:
        return 'status-todo';
      case TaskStatus.IN_PROGRESS:
        return 'status-inprogress';
      case TaskStatus.FINISHED:
        return 'status-finished';
      default:
        return '';
    }
  }
  
  /**
   * Format date for display
   * @param dateInput Date string or Date object to format
   * @returns Formatted date string
   */
  formatDate(dateInput: string | Date | undefined): string {
    if (!dateInput) return 'Nie ustawiono';
    const date = typeof dateInput === 'string' ? new Date(dateInput) : dateInput;
    return date.toLocaleDateString('pl-PL');
  }
  
  /**
   * Navigate to task edit page
   */
  editTask(taskId: string): void {
    // Navigation handled via routerLink in template
  }

  /**
   * Returns tasks filtered by status
   * @param tasks Array of tasks to filter
   * @param status Status to filter by
   * @returns Filtered array of tasks
   */
  getFilteredTasks(tasks: Task[], status: TaskStatus): Task[] {
    if (!tasks || !tasks.length) {
      return [];
    }
    return tasks.filter(task => task.status === status);
  }

  /**
   * Returns count of tasks with specified status
   * @param tasks Array of tasks to count
   * @param status Status to count
   * @returns Number of tasks with specified status
   */
  getFilteredTasksCount(tasks: Task[], status: TaskStatus): number {
    if (!tasks || !tasks.length) {
      return 0;
    }
    return this.getFilteredTasks(tasks, status).length;
  }
}

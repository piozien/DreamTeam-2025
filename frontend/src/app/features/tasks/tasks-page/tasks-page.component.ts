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
import { Project, ProjectMemberDTO } from '../../../shared/models/project.model';
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
  expandedTasks: Set<string> = new Set<string>();
  
  // Store user and task information for display
  userMap: Map<string, string> = new Map<string, string>();
  taskMap: Map<string, string> = new Map<string, string>();
  projectMembersMap: Map<string, ProjectMemberDTO[]> = new Map<string, ProjectMemberDTO[]>();
  assignmentToUserMap: Map<string, string> = new Map<string, string>();
  
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
   * Toggle task expansion state
   * @param taskId ID of the task to toggle
   */
  toggleTaskExpansion(taskId: string): void {
    if (this.expandedTasks.has(taskId)) {
      this.expandedTasks.delete(taskId);
    } else {
      this.expandedTasks.add(taskId);
      // Load additional data when expanding
      this.loadTaskDetails(taskId);
    }
  }

  /**
   * Check if a task is expanded
   * @param taskId Task ID to check
   * @returns true if task is expanded, false otherwise
   */
  isTaskExpanded(taskId: string): boolean {
    return this.expandedTasks.has(taskId);
  }

  /**
   * Load additional details for the task when expanded
   * @param taskId Task ID to load details for
   */
  loadTaskDetails(taskId: string): void {
    const task = this.findTaskById(taskId);
    if (!task) return;
    
    const userId = this.authService.getUserId();
    if (!userId) return;
    
    // Load project members if needed for assignee names
    if (!this.projectMembersMap.has(task.projectId)) {
      this.loadProjectMembers(task.projectId);
    }
    
    // Load task assignees to get the correct user IDs
    if (task.assigneeIds && task.assigneeIds.length > 0) {
      this.loadTaskAssignees(task.id, userId);
    }
    
    // Load task names for dependencies
    if (task.dependencyIds && task.dependencyIds.length > 0) {
      this.loadTaskNames(task.dependencyIds, userId);
    }
  }

  /**
   * Find a task by its ID from all projects
   * @param taskId Task ID to find
   * @returns The found task or undefined
   */
  findTaskById(taskId: string): Task | undefined {
    for (const projectData of this.tasksByProject) {
      const task = projectData.tasks.find(t => t.id === taskId);
      if (task) return task;
    }
    return undefined;
  }

  /**
   * Load project members for a project
   * @param projectId Project ID to load members for
   */
  loadProjectMembers(projectId: string): void {
    console.log(`Loading project members for project ${projectId}`);
    this.projectService.getProjectMembers(projectId).pipe(
      catchError(err => {
        console.error(`Error loading project members for project ${projectId}:`, err);
        return of({});
      })
    ).subscribe({
      next: (membersRecord) => {
        const members = Object.values(membersRecord);
        console.log(`Received ${members.length} project members:`, members);
        this.projectMembersMap.set(projectId, members);
        
        // Update user name map with project members
        members.forEach(member => {
          if (member && member.userId) {
            this.userMap.set(member.userId, member.name || 'Nieznany użytkownik');
            console.log(`Added user to map: ${member.userId} -> ${member.name || 'Nieznany użytkownik'}`);
          }
        });
      },
      error: (err) => {
        console.error('Error processing project members:', err);
      }
    });
  }

  /**
   * Load task names based on task IDs and store in the taskMap
   * @param taskIds Array of task IDs to load names for
   * @param userId Current user ID for authorization
   */
  loadTaskNames(taskIds: string[], userId: string): void {
    // Only load names for tasks not already in the map
    const idsToLoad = taskIds.filter(id => !this.taskMap.has(id));
    if (idsToLoad.length === 0) return;
    
    const loaders = idsToLoad.map(id => 
      this.taskService.getTaskById(id, userId).pipe(
        catchError(err => {
          console.error(`Error loading task ${id}:`, err);
          return of(null);
        })
      )
    );
    
    forkJoin(loaders).subscribe({
      next: (tasks) => {
        tasks.forEach((task, index) => {
          if (task) {
            this.taskMap.set(idsToLoad[index], task.name);
          } else {
            this.taskMap.set(idsToLoad[index], 'Nieznane zadanie');
          }
        });
      },
      error: (err) => {
        console.error('Error loading task names:', err);
      }
    });
  }

  /**
   * Get user name from user ID using the cached map
   * @param userId User ID to get name for
   * @returns User name or placeholder text
   */
  getUserName(userId: string): string {
    return this.userMap.get(userId) || 'Nieznany użytkownik';
  }
  
  /**
   * Get assignee name from assignee ID using project members
   * @param assigneeId Assignee ID to get name for
   * @param projectId Project ID to find members in
   * @returns User name or placeholder text
   */
  getAssigneeName(assignmentId: string, projectId: string): string {
    // First convert assignment ID to user ID
    const userId = this.assignmentToUserMap.get(assignmentId);
    if (!userId) {
      return 'Ładowanie...';
    }
    
    // Then find the user in project members
    const members = this.projectMembersMap.get(projectId);
    if (!members) {
      return 'Ładowanie...';
    }

    const member = members.find(m => m.userId === userId);
    if (member && member.name) {
      return member.name;
    }
    
    // Fallback to the user map if we have it there
    return this.userMap.get(userId) || 'Nieznany użytkownik';
  }

  /**
   * Get task name from task ID using the cached map
   * @param taskId Task ID to get name for
   * @returns Task name or placeholder text
   */
  getTaskName(taskId: string): string {
    return this.taskMap.get(taskId) || 'Ładowanie...';
  }

  /**
   * Load task assignees to get the mapping between assignment IDs and user IDs
   * @param taskId Task ID to load assignees for
   * @param userId Current user ID for authorization
   */
  loadTaskAssignees(taskId: string, userId: string): void {
    this.taskService.getTaskAssignees(taskId, userId).pipe(
      catchError(err => {
        console.error(`Error loading assignees for task ${taskId}:`, err);
        return of([]);
      })
    ).subscribe({
      next: (assignees) => {
        console.log(`Received ${assignees.length} assignees for task ${taskId}:`, assignees);
        
        // Create mapping from assignment ID to user ID
        assignees.forEach(assignee => {
          if (assignee && assignee.id && assignee.userId) {
            this.assignmentToUserMap.set(assignee.id, assignee.userId);
            console.log(`Mapped assignment ID ${assignee.id} to user ID ${assignee.userId}`);
          }
        });
      },
      error: (err) => {
        console.error('Error processing task assignees:', err);
      }
    });
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

import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE, MatNativeDateModule } from '@angular/material/core';
import { Project, ProjectCreate, ProjectUserRole } from '../../../shared/models/project.model';
import { ProjectService } from '../../../shared/services/project.service';
import { WebSocketService } from '../../../shared/services/websocket.service';
import { ProjectDialogComponent } from './project-dialog/project-dialog.component';
import { Subscription, Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ToastNotificationService } from '../../../shared/services/toast-notification.service';
import { AuthService } from '../../../shared/services/auth.service';


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
  selector: 'app-project-panel',
  templateUrl: './project-panel.component.html',
  styleUrls: ['./project-panel.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatNativeDateModule,
    MatDividerModule
  ],
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'pl-PL' },
    { provide: MAT_DATE_FORMATS, useValue: MY_DATE_FORMATS }
  ]
})
export class ProjectPanelComponent implements OnInit, OnDestroy {
  projects: Project[] = [];
  assignedProjects: Project[] = [];
  unassignedProjects: Project[] = [];
  selectedProject: Project | null = null;
  projectRoles = Object.values(ProjectUserRole);
  loading = false;
  error: string | null = null;
  isCreating = false;
  private subscription: Subscription = new Subscription();
  private currentUserId: string | null = null;

  constructor(
    private projectService: ProjectService,
    private router: Router,
    private dialog: MatDialog,
    private toastService: ToastNotificationService,
    private websocketService: WebSocketService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.authService.getUserId();
    this.loadProjects();
    this.subscribeToNotifications();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Subscribe to WebSocket notifications
   */
  private subscribeToNotifications(): void {
    // Subscribe to notifications from WebSocket
    this.subscription.add(
      this.websocketService.notifications$.subscribe(notification => {
        console.log('Received notification in project panel:', notification);
        
        // Instead of handling notifications here, navbar component now handles all toasts
        // Just handle actions like refreshing lists
        if (notification && notification.status) {
          // Project-related notifications
          switch(notification.status) {
            case 'PROJECT_CREATED':
            case 'PROJECT_UPDATED':
            case 'PROJECT_DELETED':
              this.loadProjects(); // Refresh projects list
              break;
          }
        }
      })
    );
  }

  loadProjects(): void {
    this.loading = true;
    this.error = null;

    this.subscription.add(
      this.projectService.getProjects().subscribe({
        next: (projects) => {
          console.log('Projects loaded successfully:', projects.length);
          this.projects = projects || [];
          
          // Start categorizing projects, which sets its own loading state
          this.categorizeProjects();
        },
        error: (error) => {
          console.error('Error loading projects:', error);
          this.loading = false;
          
          // Make sure we have empty arrays if projects failed to load
          this.projects = [];
          this.assignedProjects = [];
          this.unassignedProjects = [];
          
          // Check if this is an authentication error
          if (error && error.message === 'AUTHENTICATION_REQUIRED') {
            this.error = 'Wymagane zalogowanie się do systemu.';
            // Error will be set in the UI without toast notification
            // Redirect to login page once, not in a loop
            this.router.navigate(['/auth/login']);
          } else {
            this.error = 'Wystąpił błąd podczas ładowania projektów.';
            // Error will be set in the UI without toast notification
          }
        }
      })
    );
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(ProjectDialogComponent, {
      width: '500px',
      data: {
        title: 'Nowy Projekt',
        submitButton: 'Utwórz'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.createProject(result);
      }
    });
  }

  openEditDialog(project: Project): void {
    const dialogRef = this.dialog.open(ProjectDialogComponent, {
      width: '500px',
      data: {
        title: 'Edytuj Projekt',
        submitButton: 'Zapisz',
        isEditMode: true  // Enable edit mode to allow keeping past start dates
      }
    });
    
    // Pre-fill the form with current project data - this needs to be done after dialog creation
    const dialogComponent = dialogRef.componentInstance;
    const originalStartDate = new Date(project.startDate);
    
    // Set project data
    dialogComponent.project = {
      name: project.name,
      description: project.description,
      startDate: originalStartDate,
      endDate: project.endDate ? new Date(project.endDate) : undefined
    };
    
    // Set the original start date to prevent choosing earlier dates
    dialogComponent.setOriginalStartDate(originalStartDate);

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.updateProject(project.id, result);
      }
    });
  }

  /**
   * Categorizes projects into assigned and unassigned based on current user
   */
  categorizeProjects(): void {
    if (!this.currentUserId) {
      console.warn('No current user ID available for project categorization');
      this.assignedProjects = [];
      this.unassignedProjects = this.projects;
      return;
    }

    // Debug the structure of the first project to understand what we're working with
    if (this.projects.length > 0) {
      console.log('Project structure sample:', JSON.stringify(this.projects[0], null, 2));
    }

    // Initialize arrays
    this.assignedProjects = [];
    this.unassignedProjects = [];
    this.loading = true;

    // Create an Observable array of project member requests
    const memberRequests: Observable<{project: Project, isMember: boolean}>[] = [];
    
    // For each project, check if the current user is a member
    for (const project of this.projects) {
      const memberCheck$ = this.projectService.getProjectMembers(project.id).pipe(
        map(members => {
          // Check if the current user is in the members list
          const memberValues = Object.values(members);
          const userIsMember = memberValues.some(member => member.userId === this.currentUserId);
          return { project, isMember: userIsMember };
        }),
        catchError(error => {
          console.error(`Error fetching members for project ${project.id}:`, error);
          return of({ project, isMember: false });
        })
      );
      memberRequests.push(memberCheck$);
    }
    
    // If there are no projects, set loading to false and return
    if (memberRequests.length === 0) {
      this.loading = false;
      return;
    }

    // Use forkJoin to wait for all requests to complete
    this.subscription.add(
      forkJoin(memberRequests).subscribe({
        next: (results) => {
          // Categorize projects based on membership
          for (const result of results) {
            if (result.isMember) {
              this.assignedProjects.push(result.project);
            } else {
              this.unassignedProjects.push(result.project);
            }
          }
          console.log(`Categorized ${this.assignedProjects.length} assigned projects and ${this.unassignedProjects.length} unassigned projects`);
          this.loading = false;
        },
        error: (error) => {
          console.error('Error categorizing projects:', error);
          // On error, put all projects in unassigned category as fallback
          this.assignedProjects = [];
          this.unassignedProjects = this.projects;
          this.loading = false;
        }
      })
    );
  }

  createProject(projectData: Partial<Project>): void {
    this.isCreating = true;
    console.log('Project data from dialog:', projectData);

    const newProject: ProjectCreate = {
      name: projectData.name || '',
      description: projectData.description || '',
      startDate: projectData.startDate || new Date(),
      endDate: projectData.endDate
    };

    this.subscription.add(
      this.projectService.createProject(newProject).subscribe({
        next: (project) => {
          this.projects.push(project);
          this.categorizeProjects(); // Recategorize projects
          
          // Add a delay before navigation to allow toast to be seen
          setTimeout(() => {
            this.selectProject(project);
          }, 1500); // 1.5 second delay
          this.isCreating = false;
          // No toast here - will be handled by WebSocket notification
        },
        error: (error) => {
          console.error('Error creating project:', error);
          this.error = 'Wystąpił błąd podczas tworzenia projektu.';
          this.isCreating = false;
          
          // Error will be set in the UI without toast notification
        }
      })
    );
  }

  updateProject(projectId: string, projectData: Partial<Project>): void {
    this.loading = true;
    console.log('Project update data:', projectData);
    const updatedProject = {
      ...projectData,
      id: projectId
    } as Project;
    
    this.subscription.add(
      this.projectService.updateProject(updatedProject).subscribe({
        next: (project) => {
          // Update the project in the array
          const index = this.projects.findIndex(p => p.id === projectId);
          if (index !== -1) {
            this.projects[index] = project;
          }
          this.categorizeProjects(); // Recategorize projects
          this.loading = false;
          // No toast here - will be handled by WebSocket notification
        },
        error: (error) => {
          console.error('Error updating project:', error);
          this.error = 'Wystąpił błąd podczas aktualizacji projektu.';
          this.loading = false;
          
          // Error will be set in the UI without toast notification
        }
      })
    );
  }

  deleteProject(project: Project): void {
    if (confirm(`Czy na pewno chcesz usunąć projekt "${project.name}"?`)) {
      this.loading = true;
      
      this.subscription.add(
        this.projectService.deleteProject(project.id).subscribe({
          next: () => {
            // Remove the project from the array
            this.projects = this.projects.filter(p => p.id !== project.id);
            // Update categorized projects
            this.categorizeProjects();
            this.loading = false;
            // No toast here - will be handled by WebSocket notification
          },
          error: (error) => {
            console.error('Error deleting project:', error);
            this.error = 'Wystąpił błąd podczas usuwania projektu.';
            this.loading = false;
            
            // Error will be set in the UI without toast notification
          }
        })
      );
    }
  }

  selectProject(project: Project): void {
    console.log('Selecting project:', project);
    console.log('Navigating to:', '/projects/' + project.id);
    this.router.navigate(['/projects', project.id])
      .then(success => {
        console.log('Navigation result:', success);
      })
      .catch(error => {
        console.error('Navigation error:', error);
      });
  }
}
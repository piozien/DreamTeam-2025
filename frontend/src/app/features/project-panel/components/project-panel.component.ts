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
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE, MatNativeDateModule } from '@angular/material/core';
import { Project, ProjectCreate, ProjectUserRole } from '../../../shared/models/project.model';
import { ProjectService } from '../../../shared/services/project.service';
import { WebSocketService } from '../../../shared/services/websocket.service';
import { ProjectDialogComponent } from './project-dialog/project-dialog.component';
import { Subscription } from 'rxjs';
import { ToastrService } from 'ngx-toastr';


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
    MatNativeDateModule
  ],
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'pl-PL' },
    { provide: MAT_DATE_FORMATS, useValue: MY_DATE_FORMATS }
  ]
})
export class ProjectPanelComponent implements OnInit, OnDestroy {
  projects: Project[] = [];
  selectedProject: Project | null = null;
  projectRoles = Object.values(ProjectUserRole);
  loading = false;
  error: string | null = null;
  isCreating = false;
  private subscription: Subscription = new Subscription();

  constructor(
    private projectService: ProjectService,
    private router: Router,
    private dialog: MatDialog,
    private toastService: ToastrService,
    private websocketService: WebSocketService
  ) {}

  ngOnInit(): void {
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
        
        // Handle project-related notifications
        if (notification && notification.status) {
          switch(notification.status) {
            case 'PROJECT_CREATED':
              // Show toast notification for project creation
              this.toastService.success(notification.message || 'Projekt został utworzony');
              break;
              
            case 'PROJECT_UPDATED':
              this.toastService.info(notification.message || 'Projekt został zaktualizowany');
              break;
              
            case 'PROJECT_MEMBER_ADDED':
              this.toastService.info(notification.message || 'Dodano użytkownika do projektu');
              break;
              
            case 'PROJECT_MEMBER_REMOVED':
              this.toastService.info(notification.message || 'Usunięto użytkownika z projektu');
              break;
              
            case 'PROJECT_DELETED':
              this.toastService.warning(notification.message || 'Projekt został usunięty');
              break;
          }
        }
      })
    );
  }

  loadProjects(): void {
    this.loading = true;
    this.error = null;

    this.toastService.clear();
    this.subscription.add(
      this.projectService.getProjects().subscribe({
        next: (projects) => {
          this.projects = projects;
          this.toastService.success('Projekty zostały pomyślnie załadowane.');
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading projects:', error);
          this.loading = false;
          
          // Check if this is an authentication error
          if (error && error.message === 'AUTHENTICATION_REQUIRED') {
            this.error = 'Wymagane zalogowanie się do systemu.';
            this.toastService.error('Wymagane zalogowanie. Zaloguj się, aby kontynuować.');
            // Redirect to login page once, not in a loop
            this.router.navigate(['/login']);
          } else {
            this.error = 'Wystąpił błąd podczas ładowania projektów.';
            this.toastService.error('Wystąpił błąd podczas ładowania projektów.');
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
        project: { ...project } // Pass a copy of the project to edit
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.updateProject(project.id, result);
      }
    });
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

    console.log('Final project object being sent:', newProject);
    console.log('End date type:', typeof newProject.endDate);
    console.log('End date value:', newProject.endDate);

    this.subscription.add(
      this.projectService.createProject(newProject).subscribe({
        next: (project) => {
          this.projects.push(project);
          this.selectProject(project);
          this.isCreating = false;
          // Toast will come from the WebSocket notification
        },
        error: (error) => {
          console.error('Error creating project:', error);
          this.error = 'Wystąpił błąd podczas tworzenia projektu.';
          this.isCreating = false;
          this.toastService.error('Wystąpił błąd podczas tworzenia projektu.');
        }
      })
    );
  }

  updateProject(projectId: string, projectData: Partial<Project>): void {
    this.loading = true;
    console.log('Project update data:', projectData);

    // Create a partial update with only the fields we want to change
    const updatedProject = {
      id: projectId,
      name: projectData.name || '',
      description: projectData.description || '',
      startDate: projectData.startDate || new Date(),
      endDate: projectData.endDate
    };

    this.subscription.add(
      this.projectService.updateProject(updatedProject as Project).subscribe({
        next: (project) => {
          // Find and update the project in the list
          const index = this.projects.findIndex(p => p.id === projectId);
          if (index !== -1) {
            this.projects[index] = project;
          }
          this.loading = false;
          this.toastService.success('Projekt został pomyślnie zaktualizowany.');
        },
        error: (error) => {
          console.error('Error updating project:', error);
          this.error = 'Wystąpił błąd podczas aktualizacji projektu.';
          this.loading = false;
          this.toastService.error('Wystąpił błąd podczas aktualizacji projektu.');
        }
      })
    );
  }

  deleteProject(project: Project): void {
    if (!confirm('Czy na pewno chcesz usunąć ten projekt?')) return;

    this.subscription.add(
      this.projectService.deleteProject(project.id).subscribe({
        next: () => {
          this.projects = this.projects.filter(p => p.id !== project.id);
          if (this.selectedProject?.id === project.id) {
            this.selectedProject = null;
          }
        },
        error: (error) => {
          console.error('Error deleting project:', error);
          this.error = 'Wystąpił błąd podczas usuwania projektu.';
        }
      })
    );
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
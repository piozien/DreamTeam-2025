import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
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
import { ProjectDialogComponent } from './project-dialog/project-dialog.component';
import { Subscription } from 'rxjs';

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
    HttpClientModule,
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
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  loadProjects(): void {
    this.loading = true;
    this.error = null;
    this.subscription.add(
      this.projectService.getProjects().subscribe({
        next: (projects) => {
          this.projects = projects;
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading projects:', error);
          this.error = 'Wystąpił błąd podczas ładowania projektów.';
          this.loading = false;
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
        },
        error: (error) => {
          console.error('Error creating project:', error);
          this.error = 'Wystąpił błąd podczas tworzenia projektu.';
          this.isCreating = false;
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
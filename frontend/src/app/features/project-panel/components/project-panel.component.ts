import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { Project, ProjectStatus } from '../../../shared/models/project.model';
import { ProjectService } from '../../../shared/services/project.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-project-panel',
  templateUrl: './project-panel.component.html',
  styleUrls: ['./project-panel.component.scss'],
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  providers: [ProjectService]
})
export class ProjectPanelComponent implements OnInit, OnDestroy {
  projects: Project[] = [];
  selectedProject: Project | null = null;
  projectStatuses = Object.values(ProjectStatus);
  loading = false;
  error: string | null = null;
  isCreating = false;
  private subscriptions = new Subscription();

  constructor(
    private projectService: ProjectService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProjects();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  loadProjects(): void {
    this.loading = true;
    this.error = null;
    const sub = this.projectService.getProjects().subscribe(
      (projects) => {
        this.projects = projects;
        this.loading = false;
      },
      (error) => {
        console.error('Błąd podczas ładowania projektów:', error);
        this.error = 'Nie udało się załadować projektów. Spróbuj ponownie później.';
        this.loading = false;
      }
    );
    this.subscriptions.add(sub);
  }

  createProject(): void {
    if (this.isCreating) {
      console.log('Trwa tworzenie projektu, proszę czekać...');
      return;
    }

    this.isCreating = true;
    const newProject: Project = {
      id: 0,
      name: 'Nowy Projekt',
      description: 'Opis projektu',
      startDate: new Date(),
      endDate: new Date(new Date().setMonth(new Date().getMonth() + 3)),
      status: ProjectStatus.NOT_STARTED,
      teamMembers: []
    };

    const sub = this.projectService.createProject(newProject).subscribe(
      (created) => {
        this.projects = [...this.projects, created];
        this.selectProject(created);
        this.isCreating = false;
      },
      (error) => {
        console.error('Błąd podczas tworzenia projektu:', error);
        this.error = 'Nie udało się utworzyć projektu. Spróbuj ponownie.';
        this.isCreating = false;
      }
    );
    this.subscriptions.add(sub);
  }

  selectProject(project: Project): void {
    this.selectedProject = project;
  }

  deleteProject(project: Project): void {
    if (confirm(`Czy na pewno chcesz usunąć projekt "${project.name}"?`)) {
      const sub = this.projectService.deleteProject(project.id).subscribe(
        () => {
          this.projects = this.projects.filter(p => p.id !== project.id);
          if (this.selectedProject?.id === project.id) {
            this.selectedProject = null;
          }
        },
        (error) => {
          console.error('Błąd podczas usuwania projektu:', error);
          this.error = 'Nie udało się usunąć projektu. Spróbuj ponownie.';
        }
      );
      this.subscriptions.add(sub);
    }
  }

  getStatusClass(status: ProjectStatus): string {
    return status.toLowerCase().replace('_', '-');
  }

  getStatusColor(status: ProjectStatus): string {
    switch (status) {
      case ProjectStatus.NOT_STARTED:
        return '#6c757d';
      case ProjectStatus.IN_PROGRESS:
        return '#007bff';
      case ProjectStatus.ON_HOLD:
        return '#ffc107';
      case ProjectStatus.COMPLETED:
        return '#28a745';
      default:
        return '#6c757d';
    }
  }

  goToHome(): void {
    this.router.navigate(['/']);
  }
}
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { Project, ProjectUserRole } from '../../../shared/models/project.model';
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
  projectRoles = Object.values(ProjectUserRole);
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
    this.subscriptions.add(
      this.projectService.getProjects().subscribe({
        next: (projects) => {
          this.projects = projects;
          this.loading = false;
        },
        error: (error) => {
          this.error = 'Nie udało się załadować projektów';
          this.loading = false;
          console.error('Error loading projects:', error);
        }
      })
    );
  }

  selectProject(project: Project): void {
    this.selectedProject = project;
  }

  createProject(): void {
    if (this.isCreating) {
      console.log('Trwa tworzenie projektu, proszę czekać...');
      return;
    }
    
    this.isCreating = true;
    const newProject: Project = {
      id: '',
      name: 'Nowy Projekt',
      description: 'Opis projektu',
      startDate: new Date(),
      endDate: new Date(new Date().setMonth(new Date().getMonth() + 3)),
      members: []
    };

    this.subscriptions.add(
      this.projectService.createProject(newProject).subscribe({
        next: (project) => {
          this.projects.push(project);
          this.selectProject(project);
          this.isCreating = false;
        },
        error: (error) => {
          this.error = 'Nie udało się utworzyć projektu';
          this.isCreating = false;
          console.error('Error creating project:', error);
        }
      })
    );
  }

  deleteProject(project: Project): void {
    if (!confirm('Czy na pewno chcesz usunąć ten projekt?')) return;

    this.subscriptions.add(
      this.projectService.deleteProject(project.id).subscribe({
        next: () => {
          this.projects = this.projects.filter(p => p.id !== project.id);
          if (this.selectedProject?.id === project.id) {
            this.selectedProject = null;
          }
        },
        error: (error) => {
          this.error = 'Nie udało się usunąć projektu';
          console.error('Error deleting project:', error);
        }
      })
    );
  }

  goToHome(): void {
    this.router.navigate(['/']);
  }
}
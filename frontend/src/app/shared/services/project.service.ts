import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { Project, ProjectStatus } from '../../shared/models/project.model';

@Injectable()
export class ProjectService {
  private apiUrl = 'http://localhost:8080';
  private useMockData = true; // Set to true to use mock data
  private nextId = 4; // Start after our initial mock data

  private mockProjects: Project[] = [
    {
      id: 1,
      name: 'Platforma E-commerce',
      description: 'Budowa nowoczesnej platformy e-commerce z wykorzystaniem React i Spring Boot',
      startDate: new Date('2025-01-15'),
      endDate: new Date('2025-06-30'),
      status: ProjectStatus.IN_PROGRESS,
      teamMembers: [
        { id: 1, name: 'Jan Kowalski', email: 'jan@example.com', role: 'Główny Programista' },
        { id: 2, name: 'Anna Nowak', email: 'anna@example.com', role: 'Projektant UX' }
      ]
    },
    {
      id: 2,
      name: 'Aplikacja Mobilna',
      description: 'Tworzenie aplikacji mobilnej do śledzenia aktywności fizycznej',
      startDate: new Date('2025-03-01'),
      endDate: new Date('2025-08-15'),
      status: ProjectStatus.NOT_STARTED,
      teamMembers: [
        { id: 3, name: 'Michał Wiśniewski', email: 'michal@example.com', role: 'Programista Mobile' }
      ]
    },
    {
      id: 3,
      name: 'Migracja do Chmury',
      description: 'Migracja systemów legacy do infrastruktury chmurowej AWS',
      startDate: new Date('2025-02-01'),
      endDate: new Date('2025-04-30'),
      status: ProjectStatus.COMPLETED,
      teamMembers: [
        { id: 4, name: 'Katarzyna Zielińska', email: 'katarzyna@example.com', role: 'Inżynier DevOps' },
        { id: 5, name: 'Tomasz Brzoza', email: 'tomasz@example.com', role: 'Architekt Systemowy' }
      ]
    }
  ];

  constructor(private http: HttpClient) { }

  getProjects(): Observable<Project[]> {
    if (this.useMockData) {
      return of([...this.mockProjects]); // Return a copy to prevent mutations
    }
    // TODO: Implement backend logic
    return of([]);
  }

  getProject(id: number): Observable<Project> {
    if (this.useMockData) {
      const project = this.mockProjects.find(p => p.id === id);
      return of(project ? {...project} : {} as Project); // Return a copy
    }
    // TODO: Implement backend logic
    return of({} as Project);
  }

  createProject(project: Project): Observable<Project> {
    if (this.useMockData) {
      const newProject = {
        ...project,
        id: this.nextId++
      };
      this.mockProjects.push(newProject);
      return of({...newProject}); // Return a copy
    }
    // TODO: Implement backend logic
    return of({} as Project);
  }

  updateProject(project: Project): Observable<Project> {
    if (this.useMockData) {
      const index = this.mockProjects.findIndex(p => p.id === project.id);
      if (index !== -1) {
        this.mockProjects[index] = {...project};
        return of({...project}); // Return a copy
      }
      return of({} as Project);
    }
    // TODO: Implement backend logic
    return of({} as Project);
  }

  deleteProject(id: number): Observable<void> {
    if (this.useMockData) {
      const index = this.mockProjects.findIndex(p => p.id === id);
      if (index !== -1) {
        this.mockProjects.splice(index, 1);
      }
      return of(void 0);
    }
    // TODO: Implement backend logic
    return of(void 0);
  }
}
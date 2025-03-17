import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { Project, ProjectUserRole } from '../models/project.model';
import { User, GlobalRole } from '../models/user.model';

@Injectable()
export class ProjectService {
  private apiUrl = 'http://localhost:8080';
  private useMockData = true;
  private nextId = 4;

  private mockProjects: Project[] = [
    {
      id: '1',
      name: 'Platforma E-commerce',
      description: 'Budowa nowoczesnej platformy e-commerce z wykorzystaniem React i Spring Boot',
      startDate: new Date('2025-01-15'),
      endDate: new Date('2025-06-30'),
      members: [
        {
          id: '1',
          projectId: '1',
          user: {
            id: '1',
            name: 'Jan Kowalski',
            email: 'jan@example.com',
            globalRole: GlobalRole.CLIENT
          },
          role: ProjectUserRole.PM
        },
        {
          id: '2',
          projectId: '1',
          user: {
            id: '2',
            name: 'Anna Nowak',
            email: 'anna@example.com',
            globalRole: GlobalRole.CLIENT
          },
          role: ProjectUserRole.MEMBER
        }
      ]
    },
    {
      id: '2',
      name: 'Aplikacja Mobilna',
      description: 'Tworzenie aplikacji mobilnej do śledzenia aktywności fizycznej',
      startDate: new Date('2025-03-01'),
      endDate: new Date('2025-08-15'),
      members: [
        {
          id: '3',
          projectId: '2',
          user: {
            id: '3',
            name: 'Michał Wiśniewski',
            email: 'michal@example.com',
            globalRole: GlobalRole.CLIENT
          },
          role: ProjectUserRole.PM
        }
      ]
    },
    {
      id: '3',
      name: 'Migracja do Chmury',
      description: 'Migracja systemów legacy do infrastruktury chmurowej AWS',
      startDate: new Date('2025-02-01'),
      endDate: new Date('2025-04-30'),
      members: [
        {
          id: '4',
          projectId: '3',
          user: {
            id: '4',
            name: 'Katarzyna Zielińska',
            email: 'katarzyna@example.com',
            globalRole: GlobalRole.CLIENT
          },
          role: ProjectUserRole.PM
        },
        {
          id: '5',
          projectId: '3',
          user: {
            id: '5',
            name: 'Tomasz Brzoza',
            email: 'tomasz@example.com',
            globalRole: GlobalRole.CLIENT
          },
          role: ProjectUserRole.MEMBER
        }
      ]
    }
  ];

  constructor(private http: HttpClient) { }

  getProjects(): Observable<Project[]> {
    if (this.useMockData) {
      return of([...this.mockProjects]);
    }
    return of([]);
  }

  getProject(id: string): Observable<Project> {
    if (this.useMockData) {
      const project = this.mockProjects.find(p => p.id === id);
      return of(project ? {...project} : {
        id: '',
        name: '',
        description: '',
        startDate: new Date(),
        endDate: new Date(),
        members: []
      });
    }
    return of({
      id: '',
      name: '',
      description: '',
      startDate: new Date(),
      endDate: new Date(),
      members: []
    });
  }

  createProject(project: Project): Observable<Project> {
    if (this.useMockData) {
      const newProject = {
        ...project,
        id: String(this.nextId++)
      };
      this.mockProjects.push(newProject);
      return of({...newProject});
    }
    return of({
      id: '',
      name: '',
      description: '',
      startDate: new Date(),
      endDate: new Date(),
      members: []
    });
  }

  updateProject(project: Project): Observable<Project> {
    if (this.useMockData) {
      const index = this.mockProjects.findIndex(p => p.id === project.id);
      if (index !== -1) {
        this.mockProjects[index] = {...project};
        return of({...project});
      }
      return of({
        id: '',
        name: '',
        description: '',
        startDate: new Date(),
        endDate: new Date(),
        members: []
      });
    }
    return of({
      id: '',
      name: '',
      description: '',
      startDate: new Date(),
      endDate: new Date(),
      members: []
    });
  }

  deleteProject(id: string): Observable<void> {
    if (this.useMockData) {
      const index = this.mockProjects.findIndex(p => p.id === id);
      if (index !== -1) {
        this.mockProjects.splice(index, 1);
      }
      return of(void 0);
    }
    return of(void 0);
  }
}
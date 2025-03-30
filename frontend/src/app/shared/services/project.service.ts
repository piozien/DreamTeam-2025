import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { Project, ProjectUserRole } from '../models/project.model';
import { User, GlobalRole } from '../models/user.model';

interface ProjectsData {
  projects: Project[];
}

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private apiUrl = 'http://localhost:8080';
  private jsonUrl = 'assets/data/projects.json';
  private useMockData = true;
  private cachedProjects: Project[] = [];

  constructor(private http: HttpClient) {}

  getProjects(): Observable<Project[]> {
    if (this.useMockData) {
      if (this.cachedProjects.length > 0) {
        return of(this.cachedProjects);
      }
      
      return this.http.get<ProjectsData>(this.jsonUrl).pipe(
        map(data => {
          // Convert string dates to Date objects
          const projects = data.projects.map(project => ({
            ...project,
            startDate: new Date(project.startDate),
            endDate: new Date(project.endDate)
          }));
          this.cachedProjects = projects;
          return projects;
        }),
        catchError(error => {
          console.error('Error loading projects from JSON:', error);
          return of([]);
        })
      );
    }
    return this.http.get<Project[]>(`${this.apiUrl}/projects`);
  }

  getProject(id: string): Observable<Project | undefined> {
    if (this.useMockData) {
      return this.getProjects().pipe(
        map(projects => projects.find(p => p.id === id))
      );
    }
    return this.http.get<Project>(`${this.apiUrl}/projects/${id}`);
  }

  createProject(project: Project): Observable<Project> {
    if (this.useMockData) {
      return this.getProjects().pipe(
        map(projects => {
          const newId = (Math.max(...projects.map(p => parseInt(p.id))) + 1).toString();
          const newProject = { ...project, id: newId };
          this.cachedProjects = [...projects, newProject];
          this.saveProjectsToJson(this.cachedProjects);
          return newProject;
        })
      );
    }
    return this.http.post<Project>(`${this.apiUrl}/projects`, project);
  }

  updateProject(project: Project): Observable<Project> {
    if (this.useMockData) {
      return this.getProjects().pipe(
        map(projects => {
          const index = projects.findIndex(p => p.id === project.id);
          if (index !== -1) {
            this.cachedProjects = [
              ...projects.slice(0, index),
              project,
              ...projects.slice(index + 1)
            ];
            this.saveProjectsToJson(this.cachedProjects);
          }
          return project;
        })
      );
    }
    return this.http.put<Project>(`${this.apiUrl}/projects/${project.id}`, project);
  }

  deleteProject(id: string): Observable<void> {
    if (this.useMockData) {
      return this.getProjects().pipe(
        map(projects => {
          this.cachedProjects = projects.filter(p => p.id !== id);
          this.saveProjectsToJson(this.cachedProjects);
        })
      );
    }
    return this.http.delete<void>(`${this.apiUrl}/projects/${id}`);
  }

  private saveProjectsToJson(projects: Project[]): void {
    // In a real application, this would be an API call
    // For now, we'll just update our cache and log the changes
    console.log('Projects to be saved:', projects);
    this.cachedProjects = projects;
  }
}
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { Project, ProjectCreate } from '../models/project.model';
import { User, GlobalRole } from '../models/user.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private apiUrl = 'http://localhost:8080/api/projects';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getUserParams(): HttpParams {
    const currentUser = this.authService.getCurrentUser()
    let params = new HttpParams();
    if (currentUser) {
      params = params.set('userId', currentUser.id)
    }
    return params;
  }

  getProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.apiUrl}`, {params: this.getUserParams()});
  }

  getProject(id: string): Observable<Project | undefined> {
    return this.http.get<Project>(`${this.apiUrl}/${id}`, {params: this.getUserParams()});
  }

  createProject(project: ProjectCreate): Observable<Project> {
    return this.http.post<Project>(`${this.apiUrl}`, project, {params: this.getUserParams()});
  }

  updateProject(project: Project): Observable<Project> {
    return this.http.put<Project>(`${this.apiUrl}/${project.id}`, project, {params: this.getUserParams()});
  }

  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {params: this.getUserParams()});
  }
}
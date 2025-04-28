import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError, tap } from 'rxjs/operators';
import { Project, ProjectCreate, ProjectMemberDTO, ProjectUserRole, AddProjectMemberDTO, UpdateProjectMemberRoleDTO } from '../models/project.model';
import { User } from '../models/user.model';
import { GlobalRole } from '../enums/global-role.enum';
import { AuthService } from './auth.service';
import { DatePipe } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private apiUrl = 'http://localhost:8080/api/projects';

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private datePipe: DatePipe
  ) {}

  private getUserParams(): HttpParams {
    const currentUser = this.authService.getUserId();
    let params = new HttpParams();
    if (currentUser) {
      params = params.set('userId', currentUser);
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
    // Klonujemy projekt, aby nie modyfikować oryginalnego obiektu
    const formattedProject = { ...project };
    
    if (formattedProject.startDate) {
      formattedProject.startDate = this.formatDate(formattedProject.startDate);
    }
    
    if (formattedProject.endDate) {
      formattedProject.endDate = this.formatDate(formattedProject.endDate);
    }
    
    return this.http.post<Project>(`${this.apiUrl}`, formattedProject, {params: this.getUserParams()});
  }

  updateProject(project: Project): Observable<Project> {
    // Klonujemy projekt, aby nie modyfikować oryginalnego obiektu
    const formattedProject = { ...project };

    if (formattedProject.startDate) {
      formattedProject.startDate = this.formatDate(formattedProject.startDate);
    }
    
    if (formattedProject.endDate) {
      formattedProject.endDate = this.formatDate(formattedProject.endDate);
    }
    
    return this.http.put<Project>(`${this.apiUrl}/${project.id}`, formattedProject, {params: this.getUserParams()});
  }

  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {params: this.getUserParams()});
  }

  addProjectMember(projectId: string, memberData: AddProjectMemberDTO): Observable<ProjectMemberDTO> {
    console.log('Adding project member:', projectId, memberData);
    const currentUser = this.authService.getCurrentUser();
    let params = new HttpParams();
    if (currentUser) {
      params = params.set('currentUserId', currentUser.id);
    }
    console.log('Adding member with params:', params.toString());
    return this.http.post<ProjectMemberDTO>(`${this.apiUrl}/${projectId}/members`, memberData, { params: params });
  }

  removeProjectMember(projectId: string, userId: string): Observable<void> {
    const currentUser = this.authService.getCurrentUser();
    let params = new HttpParams();
    if (currentUser) {
      params = params.set('currentUserId', currentUser.id);
    }
    console.log('Removing member with params:', params.toString());
    return this.http.delete<void>(`${this.apiUrl}/${projectId}/members/${userId}`, {params: params});
  }

  updateMemberRole(projectId: string, userId: string, newRole: ProjectUserRole): Observable<ProjectMemberDTO> {
    const roleData: UpdateProjectMemberRoleDTO = { role: newRole };
    const currentUser = this.authService.getCurrentUser();
    let params = new HttpParams();
    if (currentUser) {
      params = params.set('currentUserId', currentUser.id);
    }
    console.log('Updating member role with params:', params.toString());
    return this.http.put<ProjectMemberDTO>(`${this.apiUrl}/${projectId}/members/${userId}`, roleData, {params: params});
  }

  getProjectMembers(projectId: string): Observable<Record<string, ProjectMemberDTO>> {
    return this.http.get<Record<string, ProjectMemberDTO>>(`${this.apiUrl}/${projectId}/members`, {params: this.getUserParams()});
  }
  
  /**
   * Formatuje obiekt Date do formatu yyyy-mm-dd
   * @param date obiekt Date do sformatowania
   * @returns sformatowana data w formacie yyyy-mm-dd
   */
  private formatDate(date: Date): any {
    return this.datePipe.transform(date, 'yyyy-MM-dd');
  }
}
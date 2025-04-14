import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { Project, ProjectStatus, ProjectUserRole, ProjectMemberDTO, AddProjectMemberDTO } from '../../../shared/models/project.model';
import { User } from '../../../shared/models/user.model';
import { ProjectService } from '../../../shared/services/project.service';
// Import dialog components - they'll be referenced through the dialog service
import { ProjectDialogComponent } from '../../../features/project-panel/components/project-dialog/project-dialog.component';
import { MemberDialogComponent } from './member-dialog/member-dialog.component';

@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrls: ['./project-view.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatTabsModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatNativeDateModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    MatSnackBarModule
  ],
})
export class ProjectViewComponent implements OnInit, OnDestroy {
  projectId: string | null = null;
  project: Project | null = null;
  projectMembers: any[] = [];
  loading = true;
  error: string | null = null;
  isEditing = false;
  isAddingMember = false;
  isPerformingMemberAction = false;
  
  private subscription: Subscription = new Subscription();
  
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}
  
  ngOnInit(): void {
    this.projectId = this.route.snapshot.paramMap.get('id');
    if (this.projectId) {
      this.loadProject(this.projectId);
    } else {
      this.error = 'Nie znaleziono identyfikatora projektu';
      this.loading = false;
    }
  }
  
  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }
  
  loadProject(id: string): void {
    this.loading = true;
    this.error = null;
    
    this.subscription.add(
      this.projectService.getProject(id).subscribe({
        next: (project) => {
          if (project) {
            this.project = project;
            if (project.members) {
              this.projectMembers = project.members;
            }
            this.loadProjectMembers(id);
          } else {
            this.error = 'Nie znaleziono projektu';
          }
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading project:', error);
          this.error = 'Wystąpił błąd podczas ładowania projektu.';
          this.loading = false;
        }
      })
    );
  }

  loadProjectMembers(projectId: string): void {
    this.subscription.add(
      this.projectService.getProjectMembers(projectId).subscribe({
        next: (membersMap) => {
          if (membersMap) {
            // Convert the map to an array of project members
            this.projectMembers = Object.values(membersMap).map(dto => {
              return {
                id: dto.id,
                projectId: dto.projectId,
                role: dto.role,
                user: { 
                  id: dto.userId,
                  name: dto.name  // Use the name field returned by the API
                }
              };
            });
          }
        },
        error: (error) => {
          console.error('Error loading project members:', error);
          this.snackBar.open('Problem z wczytaniem członków projektu', 'Zamknij', { duration: 3000 });
        }
      })
    );
  }
  getStatusDisplayName(status: ProjectStatus): string {
    switch (status) {
      case ProjectStatus.PLANNED: return 'Zaplanowany';
      case ProjectStatus.IN_PROGRESS: return 'W trakcie';
      case ProjectStatus.COMPLETED: return 'Zakończony';
      default: return status as string;
    }
  }

  getRoleDisplayName(role: ProjectUserRole): string {
    switch (role) {
      case ProjectUserRole.PM: return 'Manager Projektu';
      case ProjectUserRole.MEMBER: return 'Członek';
      case ProjectUserRole.CLIENT: return 'Klient';
      default: return role as string;
    }
  }
  
  goBack(): void {
    this.router.navigate(['/projects']);
  }

  openEditDialog(): void {
    if (!this.project) return;
    
    this.isEditing = true;
    
    const dialogRef = this.dialog.open(ProjectDialogComponent, {
      width: '500px',
      data: {
        title: 'Edytuj projekt',
        submitButton: 'Zapisz'
      }
    });
    
    // Pre-fill the form with current project data
    const dialogComponent = dialogRef.componentInstance;
    dialogComponent.project = {
      name: this.project.name,
      description: this.project.description,
      startDate: new Date(this.project.startDate),
      endDate: this.project.endDate ? new Date(this.project.endDate) : undefined
    };
    
    dialogRef.afterClosed().subscribe(result => {
      this.isEditing = false;
      
      if (result && this.project && this.projectId) {
        const updatedProject: Project = {
          ...this.project,
          name: result.name,
          description: result.description,
          startDate: result.startDate,
          endDate: result.endDate
        };
        
        this.subscription.add(
          this.projectService.updateProject(updatedProject).subscribe({
            next: (project) => {
              this.project = project;
              this.snackBar.open('Projekt został zaktualizowany', 'Zamknij', { duration: 3000 });
            },
            error: (error) => {
              console.error('Error updating project:', error);
              this.error = 'Wystąpił błąd podczas aktualizacji projektu.';
              this.snackBar.open('Problem z aktualizacją projektu', 'Zamknij', { duration: 3000 });
            }
          })
        );
      }
    });
  }

  openAddMemberDialog(): void {
    if (!this.project || !this.projectId) return;
    
    this.isAddingMember = true;
    
    const dialogRef = this.dialog.open(MemberDialogComponent, {
      width: '500px',
      data: {
        title: 'Dodaj członka projektu',
        submitButton: 'Dodaj',
        mode: 'add'
      }
    });
    
    dialogRef.afterClosed().subscribe(result => {
      this.isAddingMember = false;
      
      if (result && this.projectId) {
        console.log('Dialog result:', result);
        
        // Ensure userId is a string
        const userId = result.userId ? result.userId.toString() : '';
        
        const memberData: AddProjectMemberDTO = {
          userId: userId,
          role: result.role
        };
        
        console.log('Adding member with data:', memberData);
        
        this.subscription.add(
          this.projectService.addProjectMember(this.projectId as string, memberData).subscribe({
            next: (memberDTO) => {
              console.log('Member added successfully:', memberDTO);
              // Refresh the project members list
              this.loadProjectMembers(this.projectId as string);
              this.snackBar.open('Członek został dodany do projektu', 'Zamknij', { duration: 3000 });
            },
            error: (error) => {
              console.error('Error adding project member:', error);
              let errorMessage = 'Problem z dodaniem członka projektu';
              if (error.error && error.error.message) {
                errorMessage += ': ' + error.error.message;
              }
              this.snackBar.open(errorMessage, 'Zamknij', { duration: 5000 });
            }
          })
        );
      }
    });
  }

  openEditRoleDialog(member: any): void {
    if (!this.project || !this.projectId) return;
    
    this.isPerformingMemberAction = true;
    
    const dialogRef = this.dialog.open(MemberDialogComponent, {
      width: '500px',
      data: {
        title: 'Zmień rolę członka',
        submitButton: 'Zapisz',
        mode: 'edit',
        userId: member.user.id,
        currentRole: member.role
      }
    });
    
    dialogRef.afterClosed().subscribe(result => {
      this.isPerformingMemberAction = false;
      
      if (result && this.projectId && member.user && member.user.id) {
        this.subscription.add(
          this.projectService.updateMemberRole(this.projectId as string, member.user.id, result.role).subscribe({
            next: (updatedMember) => {
              // Update the member in the local array
              const index = this.projectMembers.findIndex(m => m.id === member.id);
              if (index !== -1) {
                this.projectMembers[index].role = result.role;
              }
              this.snackBar.open('Rola członka została zaktualizowana', 'Zamknij', { duration: 3000 });
            },
            error: (error) => {
              console.error('Error updating member role:', error);
              this.snackBar.open('Problem z aktualizacją roli członka', 'Zamknij', { duration: 3000 });
            }
          })
        );
      }
    });
  }

  confirmRemoveMember(member: any): void {
    if (!this.project || !this.projectId || !member.user || !member.user.id) return;
    
    if (confirm(`Czy na pewno chcesz usunąć ${member.user.name || 'użytkownika'} z projektu?`)) {
      this.isPerformingMemberAction = true;
      
      this.subscription.add(
        this.projectService.removeProjectMember(this.projectId as string, member.user.id).subscribe({
          next: () => {
            // Remove member from the local array
            this.projectMembers = this.projectMembers.filter(m => m.id !== member.id);
            this.isPerformingMemberAction = false;
            this.snackBar.open('Członek został usunięty z projektu', 'Zamknij', { duration: 3000 });
          },
          error: (error) => {
            console.error('Error removing project member:', error);
            this.isPerformingMemberAction = false;
            this.snackBar.open('Problem z usunięciem członka projektu', 'Zamknij', { duration: 3000 });
          }
        })
      );
    }
  }
}

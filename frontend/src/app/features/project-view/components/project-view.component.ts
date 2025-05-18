import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild } from '@angular/core';
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
import { MatMenuModule } from '@angular/material/menu';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { Subscription } from 'rxjs';
import { Project, ProjectStatus, ProjectUserRole, ProjectMemberDTO, AddProjectMemberDTO } from '../../../shared/models/project.model';
import { Task, TaskPriority, TaskStatus, TaskAssignee } from '../../../shared/models/task.model';
import { User } from '../../../shared/models/user.model';
import { ProjectService } from '../../../shared/services/project.service';
import { TaskService } from '../../../shared/services/task.service';
import { AuthService } from '../../../shared/services/auth.service';
// Import dialog components - they'll be referenced through the dialog service
import { ProjectDialogComponent } from '../../../features/project-panel/components/project-dialog/project-dialog.component';
import { MemberDialogComponent } from './member-dialog/member-dialog.component';
import { TaskDialogComponent } from './task-dialog/task-dialog.component';
import { ToastrService } from 'ngx-toastr';
import { ProjectCalendarComponent } from './project-calendar/project-calendar.component';

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
    MatSnackBarModule,
    MatMenuModule,
    MatChipsModule,
    MatBadgeModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    ProjectCalendarComponent
  ],
})
export class ProjectViewComponent implements OnInit, OnDestroy, AfterViewInit {
  projectId: string | null = null;
  project: Project | null = null;
  projectMembers: any[] = [];
  tasks: Task[] = [];
  loading = true;
  error: string | null = null;
  isEditing = false;
  isAddingMember = false;
  isAddingTask = false;
  isPerformingMemberAction = false;
  isLoadingTasks = false;
  
  // User cache to store user data and avoid redundant API calls
  private userCache: Map<string, User> = new Map<string, User>();
  
  // Table related properties
  displayedColumns: string[] = ['name', 'status', 'priority', 'startDate', 'endDate', 'assignees', 'actions'];
  dataSource = new MatTableDataSource<Task>([]);
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;
  
  private subscription: Subscription = new Subscription();
  
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
    private taskService: TaskService,
    private authService: AuthService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private toastService: ToastrService
  ) {}
  
  ngOnInit(): void {
    this.projectId = this.route.snapshot.paramMap.get('id');
    if (this.projectId) {
      this.loadProject(this.projectId);
    } else {
      this.toastService.error('Nie znaleziono identyfikatora projektu');
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
            this.loadTasks();
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

  loadTasks(): void {
    if (!this.project || !this.project.id) {
      console.error('Project not loaded or missing ID');
      return;
    }

    const userId = this.authService.getUserId();
    if (!userId) {
      console.error('No current user found');
      return;
    }

    this.isLoadingTasks = true;
    this.subscription.add(
      this.taskService.getTasksByProject(this.project.id, userId).subscribe({
        next: (tasks) => {
          // Store the original tasks array
          this.tasks = tasks;
          
          // Initialize or update the data source
          if (!this.dataSource) {
            this.dataSource = new MatTableDataSource<Task>(this.tasks);
          } else {
            this.dataSource.data = this.tasks;
          }
          
          // Configure sorting and filtering
          this.setupDataSource();
          
          // Make sure pagination works correctly
          this.setupTableControls();
          
          this.isLoadingTasks = false;
        },
        error: (error) => {
          console.error('Error loading tasks:', error);
          this.isLoadingTasks = false;
          this.snackBar.open('Problem z załadowaniem zadań', 'Zamknij', { duration: 3000 });
        }
      })
    );
  }
  
  /**
   * Sets up the data source with proper filtering, pagination and sorting
   */
  private setupDataSource(): void {
    if (!this.dataSource) return;
    
    // Set up filtering predicate
    this.dataSource.filterPredicate = (data: Task, filter: string) => {
      const filterValue = filter.toLowerCase().trim();
      const name = data.name.toLowerCase();
      const status = this.getTaskStatusDisplayName(data.status).toLowerCase();
      const priority = this.getTaskPriorityDisplayName(data.priority).toLowerCase();
      
      return name.includes(filterValue) || 
             status.includes(filterValue) || 
             priority.includes(filterValue);
    };
    
    // Connect to paginator and sort if available
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;
    }
    
    if (this.sort) {
      this.dataSource.sort = this.sort;
    }
  }

  /**
   * After view init lifecycle hook - sets up the table sorting and pagination
   */
  ngAfterViewInit(): void {
    this.setupTableControls();
  }
  
  /**
   * Sets up the table controls (sorting and pagination)
   */
  private setupTableControls(): void {
    // Reset the paginator first
    if (this.paginator) {
      // Reset the page index to ensure we start at the first page
      this.paginator.pageIndex = 0;
      
      // Set default page size if not set
      if (!this.paginator.pageSize) {
        this.paginator.pageSize = 5;
      }
      
      // Customize the labels for Polish language
      this.paginator._intl.itemsPerPageLabel = 'Zadań na stronie:';
      this.paginator._intl.nextPageLabel = 'Następna strona';
      this.paginator._intl.previousPageLabel = 'Poprzednia strona';
      this.paginator._intl.firstPageLabel = 'Pierwsza strona';
      this.paginator._intl.lastPageLabel = 'Ostatnia strona';
      this.paginator._intl.getRangeLabel = (page: number, pageSize: number, length: number) => {
        if (length === 0 || pageSize === 0) {
          return `0 z ${length}`;
        }
        length = Math.max(length, 0);
        const startIndex = page * pageSize;
        const endIndex = startIndex < length ? Math.min(startIndex + pageSize, length) : startIndex + pageSize;
        return `${startIndex + 1} - ${endIndex} z ${length}`;
      };
    }

    // Add the data source connections with a small delay to ensure view stability
    setTimeout(() => {
      if (this.dataSource && this.tasks) {
        // Create a new array reference to trigger change detection
        this.dataSource.data = [...this.tasks];
        
        if (this.paginator) {
          // Connect the paginator to the data source
          this.dataSource.paginator = this.paginator;
          // Force update the page length
          this.paginator._changePageSize(this.paginator.pageSize);
        }
        
        if (this.sort) {
          this.dataSource.sort = this.sort;
        }
      }
    }, 100);
  }

  /**
   * Applies a filter to the task table
   */
  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    if (this.dataSource) {
      this.dataSource.filter = filterValue.trim().toLowerCase();
      
      if (this.dataSource.paginator) {
        this.dataSource.paginator.firstPage();
      }
    }
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
  
  getTaskStatusDisplayName(status: TaskStatus): string {
    switch (status) {
      case TaskStatus.TO_DO: return 'Do zrobienia';
      case TaskStatus.IN_PROGRESS: return 'W trakcie';
      case TaskStatus.FINISHED: return 'Zakończone';
      default: return status as string;
    }
  }
  
  getTaskPriorityDisplayName(priority: TaskPriority): string {
    switch (priority) {
      case TaskPriority.OPTIONAL: return 'Opcjonalny';
      case TaskPriority.IMPORTANT: return 'Ważny';
      case TaskPriority.CRITICAL: return 'Krytyczny';
      default: return priority as string;
    }
  }
  
  getStatusColorClass(status: TaskStatus): string {
    switch (status) {
      case TaskStatus.TO_DO: return 'status-todo';
      case TaskStatus.IN_PROGRESS: return 'status-in-progress';
      case TaskStatus.FINISHED: return 'status-done';
      default: return '';
    }
  }
  
  getPriorityColorClass(priority: TaskPriority): string {
    switch (priority) {
      case TaskPriority.OPTIONAL: return 'priority-optional';
      case TaskPriority.IMPORTANT: return 'priority-important';
      case TaskPriority.CRITICAL: return 'priority-critical';
      default: return 'priority-optional';
    }
  }

  /**
   * Get the full names of task assignees
   * @param task The task containing assignee information
   * @returns A comma-separated string of assignee names
   */
  getAssigneeNames(task: Task): string {
    if (!task || !task.assigneeIds || task.assigneeIds.length === 0) {
      return 'Brak przypisanych użytkowników';
    }

    // For simple display purposes, first fetch assignee info for these assignation IDs
    if (!task.assignees) {
      const userId = this.authService.getUserId() || '';
      // Start loading the assignees for this task if they aren't loaded
      this.subscription.add(
        this.taskService.getTaskAssignees(task.id, userId).subscribe({
          next: (assignees) => {
            // Attach the assignees to the task for future reference
            const taskRef = this.tasks.find(t => t.id === task.id);
            if (taskRef) {
              taskRef.assignees = assignees;
              this.dataSource.data = [...this.tasks];
            }
          },
          error: (error) => {
            console.error(`Error fetching assignees for task ${task.id}:`, error);
          }
        })
      );
      
      return 'Wczytywanie przypisanych użytkowników...';
    }
    
    // If we have the assignees array now (previously loaded), use it to show names
    if (task.assignees && task.assignees.length > 0) {
      const userIds = task.assignees.map((assignee: TaskAssignee) => assignee.userId);
      const names: string[] = [];
      
      userIds.forEach(userId => {
        // Try to find in cached users or project members
        if (this.userCache.has(userId)) {
          const user = this.userCache.get(userId);
          if (user) {
            names.push(user.name || `${user.firstName} ${user.lastName}`.trim());
          }
        } else {
          // Try to find in project members
          const member = this.projectMembers.find(m => m.user?.id === userId);
          if (member && member.user) {
            names.push(member.user.name || `${member.user.firstName || ''} ${member.user.lastName || ''}`.trim());
            // Cache for future reference
            this.userCache.set(userId, member.user as User);
          } else {
            // Fetch user if not found locally
            this.authService.getUserById(userId).subscribe({
              next: (user) => {
                this.userCache.set(userId, user);
                // Force refresh
                this.dataSource.data = [...this.tasks];
              },
              error: (error) => {
                console.error(`Error fetching user ${userId}:`, error);
                // Cache placeholder
                this.userCache.set(userId, {
                  id: userId,
                  name: 'Unknown User',
                  username: 'unknown',
                  email: '',
                  firstName: 'Unknown',
                  lastName: 'User',
                  globalRole: 'CLIENT' as any,
                  userStatus: 'AUTHORIZED' as any
                });
              }
            });
            // Add a temporary placeholder
            names.push('...');
          }
        }
      });
      
      return names.length > 0 ? names.join(', ') : 'Brak przypisanych użytkowników';
    }
    
    // If we have assignee objects but it's empty, there are no assignees
    return 'Brak przypisanych użytkowników';
  }

  openAddTaskDialog(): void {
    if (!this.projectId) return;
    
    this.isAddingTask = true;
    
    const dialogRef = this.dialog.open(TaskDialogComponent, {
      width: '500px',
      data: {
        title: 'Dodaj nowe zadanie',
        submitButton: 'Dodaj',
        projectId: this.projectId
      }
    });
    
    dialogRef.afterClosed().subscribe(result => {
      this.isAddingTask = false;
      
      if (result && this.projectId) {
        const userId = this.authService.getUserId() || '';
        
        console.log('Attempting to create task with data:', JSON.stringify(result, null, 2));
        console.log('Using userId:', userId);
        
        this.subscription.add(
          this.taskService.createTask(result, userId).subscribe({
            next: (createdTask) => {
              console.log('Task created successfully:', createdTask);
              this.tasks = [...this.tasks, createdTask];
              if (this.dataSource) {
                this.dataSource.data = this.tasks;
              }
              this.snackBar.open('Zadanie zostało utworzone', 'Zamknij', { duration: 3000 });
            },
            error: (error) => {
              console.error('Error creating task:', error);
              console.error('Response body:', error.error);
              this.snackBar.open(`Problem z utworzeniem zadania: ${error.status} ${error.statusText}`, 'Zamknij', { duration: 3000 });
            }
          })
        );
      }
    });
  }
  
  openEditTaskDialog(task: Task): void {
    if (!this.projectId) return;
    
    const dialogRef = this.dialog.open(TaskDialogComponent, {
      width: '500px',
      data: {
        title: 'Edytuj zadanie',
        submitButton: 'Zapisz',
        projectId: this.projectId
      }
    });
    
    // Pre-fill the form with current task data
    const dialogComponent = dialogRef.componentInstance;
    dialogComponent.task = {
      id: task.id,
      name: task.name,
      description: task.description,
      startDate: task.startDate,
      endDate: task.endDate,
      priority: task.priority,
      status: task.status
    };
    
    dialogRef.afterClosed().subscribe(result => {
      if (result && this.projectId) {
        const userId = this.authService.getUserId() || '';
        const updatedTask = {
          ...result,
          id: task.id
        };
        
        this.subscription.add(
          this.taskService.updateTask(task.id, updatedTask, userId).subscribe({
            next: (updatedTaskResponse) => {
              // Update the task in the tasks array
              this.tasks = this.tasks.map(t => t.id === updatedTaskResponse.id ? updatedTaskResponse : t);
              // Update the data source with the updated tasks
              if (this.dataSource) {
                this.dataSource.data = this.tasks;
              }
              this.snackBar.open('Zadanie zostało zaktualizowane', 'Zamknij', { duration: 3000 });
            },
            error: (error) => {
              console.error('Error updating task:', error);
              this.snackBar.open('Problem z aktualizacją zadania', 'Zamknij', { duration: 3000 });
            }
          })
        );
      }
    });
  }
  
  confirmDeleteTask(task: Task): void {
    if (confirm(`Czy na pewno chcesz usunąć zadanie: ${task.name}?`)) {
      const userId = this.authService.getCurrentUser()?.id || '';
      
      this.subscription.add(
        this.taskService.deleteTask(task.id, userId).subscribe({
          next: () => {
            this.tasks = this.tasks.filter(t => t.id !== task.id);
            // Update the data source with the filtered tasks
            if (this.dataSource) {
              this.dataSource.data = this.tasks;
            }
            this.snackBar.open('Zadanie zostało usunięte', 'Zamknij', { duration: 3000 });
          },
          error: (error) => {
            console.error('Error deleting task:', error);
            this.snackBar.open('Problem z usunięciem zadania', 'Zamknij', { duration: 3000 });
          }
        })
      );
    }
  }
  
  // This duplicate method has been replaced by the implementation above

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

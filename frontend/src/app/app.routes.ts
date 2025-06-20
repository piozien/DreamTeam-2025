import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './shared/services/auth.service';

// Admin guard function to protect admin routes
const adminGuard = () => {
  const authService = inject(AuthService);
  return authService.isAdmin() ? true : ['/projects'];
};

export const routes: Routes = [
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },
  { 
    path: 'projects', 
    loadComponent: () => import('./features/project-panel/components/project-panel.component').then(m => m.ProjectPanelComponent)
  },
  {
    path: 'projects/:id',
    loadComponent: () => import('./features/project-view/components/project-view.component').then(m => m.ProjectViewComponent)
  },
  {
    path: 'tasks',
    loadComponent: () => import('./features/tasks/tasks-page/tasks-page.component').then(m => m.TasksPageComponent)
  },
  {
    path: 'tasks/:id/edit',
    loadComponent: () => import('./features/tasks/edit-task/edit-task.component').then(m => m.EditTaskComponent)
  },
  {
    path: 'calendar',
    loadComponent: () => import('./features/calendar/calendar.component').then(m => m.CalendarComponent)
  },
  {
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'auth/oauth-callback', 
    loadComponent: () => import('./features/auth/oauth-callback/oauth-callback.component').then(m => m.OAuthCallbackComponent)
  },
  {
    path: 'auth/change-password',
    loadComponent: () => import('./features/auth/change-password/change-password.component').then(m => m.ChangePasswordComponent)
  },
  {
    path: 'admin',
    loadComponent: () => import('./features/admin/admin-panel.component').then(m => m.AdminPanelComponent),
    canMatch: [adminGuard]
  },
  { path: '**', redirectTo: 'auth/login' }
];
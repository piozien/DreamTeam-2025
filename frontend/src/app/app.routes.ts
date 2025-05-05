import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './shared/services/auth.service';

// Admin guard function to protect admin routes
const adminGuard = () => {
  const authService = inject(AuthService);
  return authService.isAdmin() ? true : ['/projects'];
};

export const routes: Routes = [
  { path: '', redirectTo: 'projects', pathMatch: 'full' },
  { 
    path: 'projects', 
    loadComponent: () => import('./features/project-panel/components/project-panel.component').then(m => m.ProjectPanelComponent)
  },
  {
    path: 'projects/:id',
    loadComponent: () => import('./features/project-view/components/project-view.component').then(m => m.ProjectViewComponent)
  },
  {
    path: 'tasks/:id/edit',
    loadComponent: () => import('./features/tasks/edit-task/edit-task.component').then(m => m.EditTaskComponent)
  },
  {
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'auth/register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'auth/oauth-callback', 
    loadComponent: () => import('./features/auth/oauth-callback/oauth-callback.component').then(m => m.OAuthCallbackComponent)
  },
  {
    path: 'admin',
    loadComponent: () => import('./features/admin/admin-panel.component').then(m => m.AdminPanelComponent),
    canMatch: [adminGuard]
  },
  { path: '**', redirectTo: 'projects' }
];
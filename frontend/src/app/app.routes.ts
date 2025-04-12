import { Routes } from '@angular/router';

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
    path: 'auth/login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'auth/register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  { path: '**', redirectTo: 'projects' }
];
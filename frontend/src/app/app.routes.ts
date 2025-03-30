import { Routes } from '@angular/router';
import { BackendComponent } from './features/backend/backend.component';
import { HomeComponent } from './features/home/components/home.component';
import { ProjectPanelComponent } from './features/project-panel/components/project-panel.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'backend', component: BackendComponent },
  { path: 'projects', component: ProjectPanelComponent }
];

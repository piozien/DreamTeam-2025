import { Routes } from '@angular/router';
import { BackendComponent } from './components/backend/backend.component';
import { HomeComponent } from './components/home/home.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'backend', component: BackendComponent }
];

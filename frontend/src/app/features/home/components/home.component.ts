import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ]
})
export class HomeComponent {
  constructor(private router: Router) {}

  navigateToProjects(): void {
    this.router.navigate(['/projects']);
  }

  navigateToTeams(): void {
    this.router.navigate(['/teams']);
  }

  navigateToTasks(): void {
    this.router.navigate(['/tasks']);
  }

  navigateToCalendar(): void {
    this.router.navigate(['/calendar']);
  }
}

import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  standalone: true,
  imports: [CommonModule]
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

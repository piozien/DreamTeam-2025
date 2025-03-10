import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  template: `
    <div style="text-align: center; padding: 20px;">
      <h1>Hello World from Angular!</h1>
      <button (click)="goToBackend()" style="padding: 10px 20px; margin-top: 20px; cursor: pointer;">
        Go to Backend
      </button>
    </div>
  `
})
export class HomeComponent {
  constructor(private router: Router) {}

  goToBackend() {
    this.router.navigate(['/backend']);
  }
}

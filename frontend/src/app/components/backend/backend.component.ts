import { Component, OnInit } from '@angular/core';
import { HelloService } from '../../services/hello.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-backend',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="text-align: center; padding: 20px;">
      <h1>{{ message }}</h1>
      <button (click)="goBack()" style="padding: 10px 20px; margin-top: 20px; cursor: pointer;">
        Go Back
      </button>
    </div>
  `
})
export class BackendComponent implements OnInit {
  message: string = 'Loading...';

  constructor(
    private helloService: HelloService,
    private router: Router
  ) {}

  ngOnInit() {
    this.helloService.getHello().subscribe({
      next: (response) => {
        this.message = response;
      },
      error: (error) => {
        console.error('Error:', error);
        this.message = 'Error loading message from backend';
      }
    });
  }

  goBack() {
    this.router.navigate(['/']);
  }
}

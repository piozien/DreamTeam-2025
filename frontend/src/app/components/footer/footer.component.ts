import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule
  ],
  template: `
    <footer class="footer">
      <mat-toolbar color="primary" class="footer-toolbar">
        <div class="footer-content">
          <div class="copyright">
            &copy; {{ currentYear }} DreamTeam-2025. All rights reserved.
          </div>
        </div>
      </mat-toolbar>
    </footer>
  `,
  styles: [`
    .footer {
      margin-top: 50px;
    }
    
    .footer-toolbar {
      padding: 16px;
    }
    
    .footer-content {
      width: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
    }
    
    .copyright {
      margin-bottom: 10px;
    }
    
    .links {
      display: flex;
      gap: 20px;
    }
    
    .links a {
      color: white;
      text-decoration: none;
    }
    
    .links a:hover {
      text-decoration: underline;
    }
    
    @media (min-width: 768px) {
      .footer-content {
        flex-direction: row;
        justify-content: space-between;
      }
      
      .copyright {
        margin-bottom: 0;
      }
    }
  `]
})
export class FooterComponent {
  currentYear = new Date().getFullYear();
}

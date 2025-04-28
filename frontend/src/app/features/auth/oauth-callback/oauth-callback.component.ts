import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../shared/services/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="oauth-callback-container">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Processing login...</span>
      </div>
      <p class="mt-3">Completing Google sign-in...</p>
      <p *ngIf="errorMessage" class="text-danger mt-2">{{ errorMessage }}</p>
    </div>
  `,
  styles: [`
    .oauth-callback-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 80vh; /* Adjust as needed */
      text-align: center;
    }
  `]
})
export class OAuthCallbackComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private routeSub: Subscription | null = null;

  errorMessage: string | null = null;

  ngOnInit(): void {
    this.routeSub = this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const error = params['error'];

      if (error) {
        this.errorMessage = 'Google authentication failed. Please try again.';
        console.error('OAuth Error:', error);
        // Redirect back to login after a delay
        setTimeout(() => this.router.navigate(['/auth/login']), 3000);
      } else if (token) {
        console.log('OAuth token received:', token);
        // Process the OAuth token using the modified service method
        // We don't need handleOAuthCallback anymore, just store the token
        this.authService.storeOAuthToken(token);
        // Redirect to home page immediately
        this.router.navigate(['/']); // Navigate to projects page
      } else {
        // No token or error found, something went wrong
        this.errorMessage = 'Invalid callback state. Redirecting to login.';
        console.error('OAuth Callback: No token or error received.');
        setTimeout(() => this.router.navigate(['/auth/login']), 3000);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.routeSub) {
      this.routeSub.unsubscribe();
    }
  }
}

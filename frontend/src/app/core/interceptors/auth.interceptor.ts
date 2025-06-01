import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../../shared/services/auth.service';

export const AuthInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {

  const authService = inject(AuthService);
  const token = authService.getToken();
  
  // Check if this is an API request that needs the auth token
  // We use 'http://localhost:8080/api/' as the base for all API requests
  const isApiUrl = req.url.startsWith('http://localhost:8080/api/');

  if (token && isApiUrl) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req);
};

import { ApplicationConfig, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { HelloService } from './shared/services/hello.service';
import { DatePipe, registerLocaleData } from '@angular/common';
import localePl from '@angular/common/locales/pl';
import { AuthService } from './shared/services/auth.service';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';

registerLocaleData(localePl);

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(
      withFetch(),
      withInterceptors([AuthInterceptor])
    ),
    HelloService,
    AuthService,
    DatePipe,
    { provide: LOCALE_ID, useValue: 'pl-PL' }
  ]
};

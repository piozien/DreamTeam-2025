import { ApplicationConfig, LOCALE_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { HelloService } from './shared/services/hello.service';
import { DatePipe, registerLocaleData } from '@angular/common';
import localePl from '@angular/common/locales/pl';
import { AuthService } from './shared/services/auth.service';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { provideToastr } from 'ngx-toastr';

registerLocaleData(localePl);

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(
      withFetch(),
      withInterceptors([AuthInterceptor])
    ),
    provideAnimations(),
    provideToastr({
      timeOut: 5000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
      closeButton: true,
      progressBar: true,
      tapToDismiss: true,
      enableHtml: false,
      easeTime: 300,
      newestOnTop: true
    }),
    HelloService,
    AuthService,
    DatePipe,
    { provide: LOCALE_ID, useValue: 'pl-PL' }
  ]
};

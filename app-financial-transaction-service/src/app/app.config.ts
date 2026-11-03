import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { apiErrorInterceptor } from './core/http/api-error.interceptor';
import { authInterceptor } from './core/http/auth.interceptor';
import { requestIdInterceptor } from './core/http/request-id.interceptor';
import { Auth0SessionService } from './core/auth/auth0-session.service';
import { SessionService } from './core/auth/session.service';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    { provide: SessionService, useExisting: Auth0SessionService },
    provideRouter(routes),
    // Order matters: the request id exists before the error interceptor reads it.
    provideHttpClient(withInterceptors([requestIdInterceptor, authInterceptor, apiErrorInterceptor])),
  ],
};

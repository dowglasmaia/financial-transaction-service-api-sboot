import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { SessionService } from '../auth/session.service';
import { ApiError } from './api-error';
import { API_BASE_URL } from './api-base-url.token';

const withToken = (request: HttpRequest<unknown>, token: string) => request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

const isApiRequest = (url: string, apiBaseUrl: string) => url === apiBaseUrl || url.startsWith(`${apiBaseUrl}/`);

const unauthenticated = (): ApiError => ({ status: 401, code: 'UNAUTHENTICATED', message: 'Your session has ended. Sign in again.' });

/**
 * Adds `Authorization: Bearer` to requests for the API only. A 401 on a GET triggers ONE silent renewal and ONE retry;
 * if that fails (or the request was a write, which is never retried) the session is marked expired and the caller gets the error.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiRequest(request.url, inject(API_BASE_URL))) return next(request);
  const session = inject(SessionService);

  const send = (token: string) => next(withToken(request, token));

  return from(session.accessToken()).pipe(
    switchMap((token) => {
      if (!token) {
        session.markExpired();
        return throwError(() => unauthenticated());
      }
      return send(token).pipe(
        catchError((error: ApiError) => {
          if (error.status !== 401) return throwError(() => error);
          if (request.method !== 'GET') {
            session.markExpired();
            return throwError(() => error);
          }
          return from(session.accessToken({ renew: true })).pipe(
            switchMap((renewed) => {
              if (!renewed) {
                session.markExpired();
                return throwError(() => error);
              }
              return send(renewed).pipe(
                catchError((retryError: ApiError) => {
                  if (retryError.status === 401) session.markExpired();
                  return throwError(() => retryError);
                }),
              );
            }),
          );
        }),
      );
    }),
  );
};

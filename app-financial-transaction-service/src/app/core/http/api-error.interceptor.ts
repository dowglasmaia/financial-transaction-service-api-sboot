import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { toApiError } from './api-error';

/** Turns every HTTP failure into a typed `ApiError`; a failure is never converted into an empty success. */
export const apiErrorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        return throwError(() => toApiError(error.status, error.error, request.headers.get('X-Request-Id') ?? undefined));
      }
      return throwError(() => error);
    }),
  );

import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { API_BASE_URL } from './api-base-url.token';

/** Adds a correlation id to API requests so an error dialog can show it for support/debugging. */
export const requestIdInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(inject(API_BASE_URL)) || request.headers.has('X-Request-Id')) return next(request);
  return next(request.clone({ setHeaders: { 'X-Request-Id': crypto.randomUUID() } }));
};

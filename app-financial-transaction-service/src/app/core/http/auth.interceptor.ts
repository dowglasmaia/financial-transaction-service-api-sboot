import { HttpInterceptorFn } from '@angular/common/http';
import { InjectionToken, inject } from '@angular/core';
import { API_BASE_URL } from './api-base-url.token';

/** Returns the current access token or null. The real provider arrives with the Okta integration (FE-03). */
export const AUTH_TOKEN_PROVIDER = new InjectionToken<() => string | null>('AUTH_TOKEN_PROVIDER', { factory: () => () => null });

/** Adds `Authorization: Bearer` to API requests only; without a token (dev-open mode) nothing is added. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const apiBaseUrl = inject(API_BASE_URL);
  const token = inject(AUTH_TOKEN_PROVIDER)();
  if (!token || !request.url.startsWith(apiBaseUrl)) return next(request);
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};

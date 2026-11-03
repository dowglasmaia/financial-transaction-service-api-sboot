import { InjectionToken, inject } from '@angular/core';
import { APP_CONFIG } from '../config/app-config';

/** Base URL of the API, taken from the runtime configuration (`/config.json`). Default: `/api/v1`. */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  factory: () => inject(APP_CONFIG).apiBaseUrl,
});

import { InjectionToken } from '@angular/core';

/** Runtime configuration, loaded from `/config.json` before the app starts (same build for every environment). */
export interface AppConfig {
  readonly apiBaseUrl: string;
  readonly demoBanner: boolean;
  /** Auth0 SPA application (public values only; a SPA has no secret). */
  readonly auth: {
    readonly domain: string;
    readonly clientId: string;
    readonly audience: string;
  };
}

export const DEFAULT_APP_CONFIG: AppConfig = {
  apiBaseUrl: '/api/v1',
  demoBanner: true,
  auth: { domain: '', clientId: '', audience: '' },
};

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG', { factory: () => DEFAULT_APP_CONFIG });

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function text(value: unknown, fallback: string): string {
  return typeof value === 'string' ? value : fallback;
}

/** Validates the untrusted JSON: unknown fields and wrong types fall back to the defaults. */
export function parseAppConfig(raw: unknown): AppConfig {
  const source = isRecord(raw) ? raw : {};
  const auth = isRecord(source['auth']) ? source['auth'] : {};
  return {
    apiBaseUrl: text(source['apiBaseUrl'], DEFAULT_APP_CONFIG.apiBaseUrl).replace(/\/+$/, '') || DEFAULT_APP_CONFIG.apiBaseUrl,
    demoBanner: typeof source['demoBanner'] === 'boolean' ? source['demoBanner'] : DEFAULT_APP_CONFIG.demoBanner,
    auth: {
      domain: text(auth['domain'], DEFAULT_APP_CONFIG.auth.domain),
      clientId: text(auth['clientId'], DEFAULT_APP_CONFIG.auth.clientId),
      audience: text(auth['audience'], DEFAULT_APP_CONFIG.auth.audience),
    },
  };
}

/** Fetches `/config.json`; any failure keeps the defaults so the app still starts. */
export async function loadAppConfig(fetchFn: typeof fetch = fetch): Promise<AppConfig> {
  try {
    const response = await fetchFn('/config.json', { cache: 'no-store' });
    if (!response.ok) return DEFAULT_APP_CONFIG;
    return parseAppConfig(await response.json());
  } catch {
    return DEFAULT_APP_CONFIG;
  }
}

import { InjectionToken } from '@angular/core';

/** Runtime configuration, loaded from `/config.json` before the app starts (same build for every environment). */
export interface AppConfig {
  readonly apiBaseUrl: string;
  readonly demoBanner: boolean;
  readonly auth: {
    readonly enabled: boolean;
    readonly issuer: string;
    readonly clientId: string;
    readonly scopes: readonly string[];
  };
}

export const DEFAULT_APP_CONFIG: AppConfig = {
  apiBaseUrl: '/api',
  demoBanner: true,
  auth: {
    enabled: false, // dev-open mode until the Okta integration (spec FE-03 / BE-04)
    issuer: '',
    clientId: '',
    scopes: ['openid', 'profile', 'email', 'transactions:read', 'transactions:write'],
  },
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
  const scopes = Array.isArray(auth['scopes']) && auth['scopes'].every((s) => typeof s === 'string') ? (auth['scopes'] as string[]) : DEFAULT_APP_CONFIG.auth.scopes;
  return {
    apiBaseUrl: text(source['apiBaseUrl'], DEFAULT_APP_CONFIG.apiBaseUrl).replace(/\/+$/, '') || DEFAULT_APP_CONFIG.apiBaseUrl,
    demoBanner: typeof source['demoBanner'] === 'boolean' ? source['demoBanner'] : DEFAULT_APP_CONFIG.demoBanner,
    auth: {
      enabled: typeof auth['enabled'] === 'boolean' ? auth['enabled'] : DEFAULT_APP_CONFIG.auth.enabled,
      issuer: text(auth['issuer'], DEFAULT_APP_CONFIG.auth.issuer),
      clientId: text(auth['clientId'], DEFAULT_APP_CONFIG.auth.clientId),
      scopes,
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

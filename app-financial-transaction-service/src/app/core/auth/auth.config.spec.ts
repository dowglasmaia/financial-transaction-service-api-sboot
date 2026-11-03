import { DEFAULT_APP_CONFIG } from '../config/app-config';
import { buildAuthConfig } from './auth.config';

const AUTH = { domain: 'tenant.example.auth0.com', clientId: 'client-123', audience: 'https://api.example.test' };

describe('buildAuthConfig', () => {
  const config = buildAuthConfig({ ...DEFAULT_APP_CONFIG, auth: AUTH }, 'http://localhost:4200');

  it('is a public SPA client: domain and client id, no secret anywhere', () => {
    expect(config.domain).toBe(AUTH.domain);
    expect(config.clientId).toBe(AUTH.clientId);
    expect(JSON.stringify(config).toLowerCase()).not.toContain('secret');
  });

  it('always sends the API audience (otherwise Auth0 returns an opaque token)', () => {
    expect(config.authorizationParams?.['audience']).toBe(AUTH.audience);
  });

  it('returns to the origin after the login and asks for a refresh token', () => {
    expect(config.authorizationParams?.['redirect_uri']).toBe('http://localhost:4200');
    expect(config.authorizationParams?.['scope']).toContain('openid');
    expect(config.authorizationParams?.['scope']).toContain('offline_access');
    expect(config.useRefreshTokens).toBe(true);
  });

  it('keeps tokens in memory, never in web storage (decision: auth-auth0.md)', () => {
    expect(config.cacheLocation).toBe('memory');
  });
});

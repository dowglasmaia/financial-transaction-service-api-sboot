import { AuthConfig } from '@auth0/auth0-angular';
import { AppConfig } from '../config/app-config';

/**
 * Auth0 SPA client configuration (Authorization Code + PKCE). Only public values: a SPA has no client secret.
 * - the API `audience` is always sent, otherwise Auth0 answers with an opaque token the API cannot validate;
 * - tokens live in memory (never in web storage); a rotating refresh token keeps the session alive;
 * - the user comes back to the origin, which must be an Allowed Callback URL of the Auth0 application.
 */
export function buildAuthConfig(config: AppConfig, origin: string): AuthConfig {
  return {
    domain: config.auth.domain,
    clientId: config.auth.clientId,
    cacheLocation: 'memory',
    useRefreshTokens: true,
    useRefreshTokensFallback: true,
    authorizationParams: {
      audience: config.auth.audience,
      redirect_uri: origin,
      scope: 'openid profile email offline_access',
    },
  };
}

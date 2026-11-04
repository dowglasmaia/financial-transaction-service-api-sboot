import { Injectable, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService as Auth0Service } from '@auth0/auth0-angular';
import { combineLatest, filter, firstValueFrom, map, take } from 'rxjs';
import { SessionService, SessionStatus, SessionUser, LoginOptions } from './session.service';

/** The provider's own words (`error_description`), else its error code, else a generic text. */
function describeError(error: unknown): string {
  const e = (error ?? {}) as { error_description?: unknown; error?: unknown; message?: unknown };
  for (const candidate of [e.error_description, e.error, e.message]) {
    if (typeof candidate === 'string' && candidate.trim() !== '') return candidate.trim();
  }
  return 'The sign-in could not be completed.';
}

/** {@link SessionService} on top of `@auth0/auth0-angular` (Authorization Code + PKCE, tokens in memory). */
@Injectable({ providedIn: 'root' })
export class Auth0SessionService extends SessionService {
  private readonly auth0 = inject(Auth0Service);
  private readonly loading = toSignal(this.auth0.isLoading$, { initialValue: true });
  private readonly authenticated = toSignal(this.auth0.isAuthenticated$, { initialValue: false });
  private readonly profile = toSignal(this.auth0.user$, { initialValue: null });
  private readonly expiredFlag = signal(false);
  private readonly refusal = toSignal(this.auth0.error$.pipe(map(describeError)), { initialValue: null });

  override readonly status = computed<SessionStatus>(() => (this.loading() ? 'loading' : this.authenticated() ? 'authenticated' : 'anonymous'));
  override readonly expired = this.expiredFlag.asReadonly();
  override readonly loginError = computed<string | null>(() => this.refusal());
  override readonly user = computed<SessionUser | null>(() => {
    const claims = this.profile();
    if (!claims) return null;
    const email = typeof claims.email === 'string' ? claims.email : '';
    const name = typeof claims.name === 'string' && claims.name ? claims.name : email || 'Customer';
    return { name, email, picture: typeof claims.picture === 'string' ? claims.picture : '' };
  });

  override whenSettled(): Promise<SessionStatus> {
    return firstValueFrom(
      combineLatest([this.auth0.isLoading$, this.auth0.isAuthenticated$]).pipe(
        filter(([loading]) => !loading),
        take(1),
        map(([, authenticated]): SessionStatus => (authenticated ? 'authenticated' : 'anonymous')),
      ),
    );
  }

  /** `prompt=login` makes the provider show its login form every time, even when an old single-sign-on session exists. */
  override login(returnTo = '/home', options?: LoginOptions): Promise<void> {
    const authorizationParams = { prompt: 'login' as const, ...(options?.loginHint ? { login_hint: options.loginHint } : {}) };
    return firstValueFrom(this.auth0.loginWithRedirect({ appState: { target: returnTo }, authorizationParams }));
  }

  override logout(): Promise<void> {
    this.expiredFlag.set(false);
    return firstValueFrom(this.auth0.logout({ logoutParams: { returnTo: window.location.origin } }));
  }

  override async accessToken(options?: { renew?: boolean }): Promise<string | null> {
    try {
      const token = await firstValueFrom(this.auth0.getAccessTokenSilently({ cacheMode: options?.renew ? 'off' : 'on' }));
      return token ?? null;
    } catch {
      return null; // login_required / consent_required / network: the caller decides (the interceptor expires the session)
    }
  }

  override markExpired(): void {
    this.expiredFlag.set(true);
  }

  override dismissExpired(): void {
    this.expiredFlag.set(false);
  }
}

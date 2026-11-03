import { Injectable, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService as Auth0Service } from '@auth0/auth0-angular';
import { combineLatest, filter, firstValueFrom, map, take } from 'rxjs';
import { SessionService, SessionStatus, SessionUser } from './session.service';

/** {@link SessionService} on top of `@auth0/auth0-angular` (Authorization Code + PKCE, tokens in memory). */
@Injectable({ providedIn: 'root' })
export class Auth0SessionService extends SessionService {
  private readonly auth0 = inject(Auth0Service);
  private readonly loading = toSignal(this.auth0.isLoading$, { initialValue: true });
  private readonly authenticated = toSignal(this.auth0.isAuthenticated$, { initialValue: false });
  private readonly profile = toSignal(this.auth0.user$, { initialValue: null });
  private readonly expiredFlag = signal(false);

  override readonly status = computed<SessionStatus>(() => (this.loading() ? 'loading' : this.authenticated() ? 'authenticated' : 'anonymous'));
  override readonly expired = this.expiredFlag.asReadonly();
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

  override login(returnTo = '/home'): Promise<void> {
    return firstValueFrom(this.auth0.loginWithRedirect({ appState: { target: returnTo } }));
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

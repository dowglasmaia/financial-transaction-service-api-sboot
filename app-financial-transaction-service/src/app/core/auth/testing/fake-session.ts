import { computed, signal } from '@angular/core';
import { SessionService, SessionStatus, SessionUser } from '../session.service';

/** Test double of the session: set `state` and `token`, inspect the calls. No library, no network. */
export class FakeSession extends SessionService {
  readonly state = signal<SessionStatus>('anonymous');
  readonly profile = signal<SessionUser | null>(null);
  readonly expiredFlag = signal(false);
  readonly loginErrorState = signal<string | null>(null);

  token: string | null = 'tok-1';
  renewedToken: string | null = 'tok-2';
  readonly tokenCalls: Array<{ renew: boolean }> = [];
  readonly loginCalls: string[] = [];
  readonly loginHints: Array<string | undefined> = [];
  logoutCalls = 0;

  override readonly status = computed(() => this.state());
  override readonly user = computed(() => this.profile());
  override readonly expired = computed(() => this.expiredFlag());
  override readonly loginError = computed(() => this.loginErrorState());

  override whenSettled(): Promise<SessionStatus> {
    return Promise.resolve(this.state());
  }

  override login(returnTo = '/home', options?: { loginHint?: string }): Promise<void> {
    this.loginCalls.push(returnTo);
    this.loginHints.push(options?.loginHint);
    return Promise.resolve();
  }

  override logout(): Promise<void> {
    this.logoutCalls++;
    this.expiredFlag.set(false);
    this.state.set('anonymous');
    this.profile.set(null);
    return Promise.resolve();
  }

  override accessToken(options?: { renew?: boolean }): Promise<string | null> {
    const renew = options?.renew === true;
    this.tokenCalls.push({ renew });
    return Promise.resolve(renew ? this.renewedToken : this.token);
  }

  override markExpired(): void {
    this.expiredFlag.set(true);
  }

  override dismissExpired(): void {
    this.expiredFlag.set(false);
  }
}

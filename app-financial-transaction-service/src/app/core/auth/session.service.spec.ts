import { TestBed } from '@angular/core/testing';
import { AuthService as Auth0Service } from '@auth0/auth0-angular';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { Auth0SessionService } from './auth0-session.service';

class FakeAuth0 {
  readonly isLoading$ = new BehaviorSubject(true);
  readonly isAuthenticated$ = new BehaviorSubject(false);
  readonly user$ = new BehaviorSubject<Record<string, unknown> | null | undefined>(null);
  loginWithRedirect = vi.fn(() => of(undefined));
  logout = vi.fn(() => of(undefined));
  getAccessTokenSilently = vi.fn((): Observable<string> => of('access-token'));
}

function setup() {
  const auth0 = new FakeAuth0();
  TestBed.configureTestingModule({ providers: [{ provide: Auth0Service, useValue: auth0 }] });
  return { auth0, session: TestBed.inject(Auth0SessionService) };
}

describe('Auth0SessionService', () => {
  it('is loading until the library has finished the redirect callback, then anonymous or authenticated', async () => {
    const { auth0, session } = setup();
    expect(session.status()).toBe('loading');

    auth0.isLoading$.next(false);
    expect(session.status()).toBe('anonymous');

    auth0.isAuthenticated$.next(true);
    expect(session.status()).toBe('authenticated');
  });

  it('whenSettled resolves once loading has finished', async () => {
    const { auth0, session } = setup();
    const settled = session.whenSettled();
    auth0.isAuthenticated$.next(true);
    auth0.isLoading$.next(false);
    expect(await settled).toBe('authenticated');
  });

  it('exposes only name, e-mail and picture of the ID token', () => {
    const { auth0, session } = setup();
    auth0.isLoading$.next(false);
    auth0.isAuthenticated$.next(true);
    auth0.user$.next({ name: 'Maria Silva', email: 'maria@example.test', picture: 'https://img.example.test/m.png', sub: 'auth0|secret-subject', nickname: 'x' });

    expect(session.user()).toEqual({ name: 'Maria Silva', email: 'maria@example.test', picture: 'https://img.example.test/m.png' });
  });

  it('falls back to the e-mail, then to "Customer", when the ID token has no name', () => {
    const { auth0, session } = setup();
    auth0.user$.next({ email: 'maria@example.test' });
    expect(session.user()?.name).toBe('maria@example.test');
    auth0.user$.next({});
    expect(session.user()?.name).toBe('Customer');
  });

  it('login goes through the library with the return target', async () => {
    const { auth0, session } = setup();
    await session.login('/home');
    expect(auth0.loginWithRedirect).toHaveBeenCalledWith({ appState: { target: '/home' } });
  });

  it('logout returns to the origin and clears an expired-session flag', async () => {
    const { auth0, session } = setup();
    session.markExpired();
    expect(session.expired()).toBe(true);

    await session.logout();

    expect(auth0.logout).toHaveBeenCalledWith({ logoutParams: { returnTo: window.location.origin } });
    expect(session.expired()).toBe(false);
  });

  it('accessToken returns the cached token by default and forces a renewal on request', async () => {
    const { auth0, session } = setup();
    await session.accessToken();
    await session.accessToken({ renew: true });
    expect(auth0.getAccessTokenSilently).toHaveBeenNthCalledWith(1, { cacheMode: 'on' });
    expect(auth0.getAccessTokenSilently).toHaveBeenNthCalledWith(2, { cacheMode: 'off' });
  });

  it('accessToken is null (not an exception, nothing logged) when the library cannot get a token', async () => {
    const { auth0, session } = setup();
    const log = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    auth0.getAccessTokenSilently.mockReturnValue(throwError(() => new Error('login_required')));

    expect(await session.accessToken()).toBeNull();
    expect(log).not.toHaveBeenCalled();
    log.mockRestore();
  });

  it('never writes tokens to the console or to web storage', async () => {
    const { auth0, session } = setup();
    const spies = (['log', 'info', 'warn', 'error', 'debug'] as const).map((method) => vi.spyOn(console, method).mockImplementation(() => undefined));
    const before = JSON.stringify({ ...localStorage }) + JSON.stringify({ ...sessionStorage });

    auth0.isLoading$.next(false);
    auth0.isAuthenticated$.next(true);
    await session.accessToken();
    await session.logout();

    spies.forEach((spy) => expect(spy).not.toHaveBeenCalled());
    expect(JSON.stringify({ ...localStorage }) + JSON.stringify({ ...sessionStorage })).toBe(before);
    spies.forEach((spy) => spy.mockRestore());
  });
});

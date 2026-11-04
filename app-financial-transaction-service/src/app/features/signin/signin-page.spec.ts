import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { SessionService } from '../../core/auth/session.service';
import { FakeSession } from '../../core/auth/testing/fake-session';
import { SignInPage } from './signin-page';

function setup(query: Record<string, string> = {}, state: 'anonymous' | 'authenticated' = 'anonymous') {
  const session = new FakeSession();
  session.state.set(state);
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      { provide: SessionService, useValue: session },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(query) } } },
    ],
  });
  const navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  const fixture = TestBed.createComponent(SignInPage);
  return { fixture, session, navigateByUrl, el: fixture.nativeElement as HTMLElement };
}

describe('SignInPage', () => {
  it('shows the welcome copy and says who handles the sign-in', async () => {
    const { fixture, el } = setup();
    await fixture.whenStable();
    expect(el.querySelector('h1')?.textContent).toContain('Welcome back');
    expect(el.textContent).toContain('Secure sign-in with Auth0');
    expect(el.querySelector('input[type="password"]')).toBeNull(); // the password is typed on Auth0's page, never here
  });

  it('starts the login through the session and then shows the redirecting state', async () => {
    const { fixture, session, el } = setup();
    await fixture.whenStable();

    el.querySelector<HTMLButtonElement>('button')!.click();
    await fixture.whenStable();

    expect(session.loginCalls).toEqual(['/home']);
    expect(session.loginHints).toEqual([undefined]); // nothing typed: no hint
    expect(el.querySelector('[role="status"]')?.textContent).toContain('Redirecting…');
    expect(el.querySelector('button')).toBeNull(); // no second click while redirecting
  });

  it('has an e-mail field (optional, only pre-fills the Auth0 form) and a submit button named "Sign in"', async () => {
    const { fixture, el } = setup();
    await fixture.whenStable();

    const email = el.querySelector<HTMLInputElement>('form input[type="email"]');
    expect(email).not.toBeNull();
    expect(email?.getAttribute('autocomplete')).toBe('username');
    expect(el.querySelector('label')?.textContent).toContain('E-mail');
    expect(el.querySelector<HTMLButtonElement>('form button[type="submit"]')?.textContent).toContain('Sign in');
  });

  it('submitting the form sends the typed e-mail as the login hint', async () => {
    const { fixture, session, el } = setup();
    await fixture.whenStable();

    const email = el.querySelector<HTMLInputElement>('input[type="email"]')!;
    email.value = '  maria@example.test ';
    email.dispatchEvent(new Event('input'));
    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(session.loginCalls).toEqual(['/home']);
    expect(session.loginHints).toEqual(['maria@example.test']);
  });

  it('a malformed e-mail is not sent: the field shows an error and no login starts', async () => {
    const { fixture, session, el } = setup();
    await fixture.whenStable();

    const email = el.querySelector<HTMLInputElement>('input[type="email"]')!;
    email.value = 'not-an-email';
    email.dispatchEvent(new Event('input'));
    el.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(session.loginCalls).toEqual([]);
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Enter a valid e-mail');
    expect(email.getAttribute('aria-invalid')).toBe('true');
  });

  it('shows the error state with a "Try again" button after a failed sign-in', async () => {
    const { fixture, el } = setup({ failed: '1' });
    await fixture.whenStable();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain("We couldn't sign you in");
    expect(el.querySelector('button')?.textContent).toContain('Try again');
  });

  it('shows what the provider said when it refused the sign-in', async () => {
    const { fixture, session, el } = setup({ failed: '1' });
    session.loginErrorState.set('Client "abc" is not authorized to access resource server "https://api.example".');
    await fixture.whenStable();

    const alert = el.querySelector('[role="alert"]')?.textContent ?? '';
    expect(alert).toContain("We couldn't sign you in");
    expect(alert).toContain('Client "abc" is not authorized to access resource server');
  });

  it('shows no error on a normal visit', async () => {
    const { fixture, el } = setup();
    await fixture.whenStable();
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('sends an already signed-in customer to Home', async () => {
    const { fixture, navigateByUrl } = setup({}, 'authenticated');
    await fixture.whenStable();
    expect(navigateByUrl).toHaveBeenCalledWith('/home', { replaceUrl: true });
  });
});

import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { SessionService } from '../../core/auth/session.service';
import { FakeSession } from '../../core/auth/testing/fake-session';
import { LandingPage } from './landing-page';

function setup(query: Record<string, string> = {}, state: 'anonymous' | 'authenticated' | 'loading' = 'anonymous') {
  const session = new FakeSession();
  session.state.set(state);
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      { provide: SessionService, useValue: session },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(query) } } },
    ],
  });
  const router = TestBed.inject(Router);
  const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
  const navigateByUrl = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  const fixture = TestBed.createComponent(LandingPage);
  return { fixture, session, navigate, navigateByUrl, el: fixture.nativeElement as HTMLElement };
}

describe('LandingPage', () => {
  it('invites the visitor to sign in and shows what the app offers', async () => {
    const { fixture, el } = setup();
    await fixture.whenStable();

    expect(el.querySelector('h1')?.textContent).toContain('Your financial life, simpler and safer.');
    const cta = el.querySelector<HTMLAnchorElement>('a[href="/signin"]');
    expect(cta?.textContent).toContain('Access my account');
    expect(el.querySelectorAll('ul[aria-label="What you can do"] li')).toHaveLength(3);
  });

  it('uses only icon weights that are loaded (the fill weight is not shipped), so no tile is empty', async () => {
    const { fixture, el } = setup();
    await fixture.whenStable();
    const icons = Array.from(el.querySelectorAll('ul[aria-label="What you can do"] i')).map((i) => i.className);
    expect(icons).toHaveLength(3);
    for (const icon of icons) expect(icon.startsWith('ph-bold ')).toBe(true);
    expect(el.innerHTML).not.toContain('ph-fill');
  });

  it('says plainly that it is a demo with fictitious data', async () => {
    const { fixture, el } = setup();
    await fixture.whenStable();
    expect(el.textContent).toContain('Demo environment · test data only');
    expect(el.textContent).toContain('fictitious');
  });

  it('while the identity provider callback is being handled it shows "Signing you in…" instead of the page', async () => {
    const { fixture, el } = setup({ code: 'abc', state: 'xyz' }, 'loading');
    await fixture.whenStable();

    expect(el.querySelector('[role="status"]')?.textContent).toContain('Signing you in…');
    expect(el.querySelector('h1')).toBeNull();
  });

  it('moves a signed-in customer to Home (replacing the URL, so the code is not left in history)', async () => {
    const { fixture, navigateByUrl } = setup({ code: 'abc', state: 'xyz' }, 'authenticated');
    await fixture.whenStable();
    expect(navigateByUrl).toHaveBeenCalledWith('/home', { replaceUrl: true });
  });

  it('does not redirect an anonymous visitor', async () => {
    const { fixture, navigateByUrl } = setup();
    await fixture.whenStable();
    expect(navigateByUrl).not.toHaveBeenCalled();
  });

  it('a failed callback (?error=…) goes to the sign-in page with the error state', async () => {
    const { fixture, navigate } = setup({ error: 'access_denied', error_description: 'x' });
    await fixture.whenStable();
    expect(navigate).toHaveBeenCalledWith(['/signin'], { queryParams: { failed: 1 }, replaceUrl: true });
  });
});

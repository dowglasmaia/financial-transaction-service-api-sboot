import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AccountApi } from '../../features/account/data-access/account.api';
import { SessionService } from '../auth/session.service';
import { FakeSession } from '../auth/testing/fake-session';
import { APP_CONFIG, DEFAULT_APP_CONFIG } from '../config/app-config';
import { AppShellComponent } from './app-shell.component';

@Component({ template: 'page' })
class DummyPage {}

const nav = (label: string, order: number) => ({ nav: { label, icon: 'ph ph-house', order } });

function routesWith(count: number) {
  const labels = ['Home', 'Transactions', 'Pix', 'Recharge', 'Deposit', 'Withdraw'];
  return labels.slice(0, count).map((label, i) => ({ path: i === 0 ? '' : label.toLowerCase(), component: DummyPage, data: nav(label, i + 1) }));
}

describe('AppShellComponent', () => {
  async function render(options: { items?: number; demoBanner?: boolean; signedIn?: boolean } = {}) {
    const session = new FakeSession();
    session.state.set(options.signedIn === false ? 'anonymous' : 'authenticated');
    if (options.signedIn !== false) session.profile.set({ name: 'Maria Silva', email: 'maria@example.test', picture: '' });
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routesWith(options.items ?? 1)),
        { provide: APP_CONFIG, useValue: { ...DEFAULT_APP_CONFIG, demoBanner: options.demoBanner ?? true } },
        { provide: SessionService, useValue: session },
        { provide: AccountApi, useValue: { me: () => of({ agency: '0211', number: '2022', balance: 5000 }) } },
      ],
    });
    const fixture = TestBed.createComponent(AppShellComponent);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, el, session };
  }

  it('has the landmarks and the skip link', async () => {
    const { el } = await render();
    expect(el.querySelector('a[href="#main"]')?.textContent).toContain('Skip to main content');
    expect(el.querySelector('main#main')).not.toBeNull();
    expect(el.querySelector('nav[aria-label="Main"]')).not.toBeNull();
    expect(el.querySelector('header')).not.toBeNull();
  });

  it('builds the rail from the route table', async () => {
    const { el } = await render({ items: 4 });
    const labels = Array.from(el.querySelectorAll('nav[aria-label="Main"] a')).map((a) => a.textContent?.trim());
    expect(labels).toEqual(['Home', 'Transactions', 'Pix', 'Recharge']);
  });

  it('shows the demo chip in the header when the config asks for it', async () => {
    const { el } = await render({ demoBanner: true });
    expect(el.querySelector('header')?.textContent).toContain('Demo environment · test data only');
  });

  it('hides the header chip and the phone strip when the config turns it off (the footer notice stays)', async () => {
    const { el } = await render({ demoBanner: false });
    expect(el.querySelector('header')?.textContent).not.toContain('Demo environment');
    expect(el.querySelector('p.sm\\:hidden')).toBeNull();
    expect(el.querySelector('app-site-footer')?.textContent).toContain('Demo environment · test data only');
  });

  it('mobile: up to three entries sit in the bottom bar, no "More" button', async () => {
    const { el } = await render({ items: 3 });
    expect(el.querySelectorAll('nav[aria-label="Quick navigation"] a')).toHaveLength(3);
    expect(el.querySelector('nav[aria-label="Quick navigation"] button')).toBeNull();
  });

  it('mobile: with more entries the rest go to a "More" dialog', async () => {
    const { fixture, el } = await render({ items: 6 });
    expect(el.querySelectorAll('nav[aria-label="Quick navigation"] a')).toHaveLength(3);
    const more = el.querySelector<HTMLButtonElement>('nav[aria-label="Quick navigation"] button');
    expect(more?.textContent).toContain('More');
    more!.click();
    await fixture.whenStable();
    const dialog = el.querySelector('[role="dialog"]');
    expect(dialog).not.toBeNull();
    expect(Array.from(dialog!.querySelectorAll('a')).map((a) => a.textContent?.trim())).toEqual(['Recharge', 'Deposit', 'Withdraw']);
  });

  it('keeps the theme toggle and the portfolio credit footer', async () => {
    const { el } = await render();
    expect(el.querySelector('app-theme-toggle button')).not.toBeNull();
    expect(el.querySelector('app-site-footer')).not.toBeNull();
  });

  describe('signed out (landing, sign-in)', () => {
    it('is the plain public frame: no rail, no bottom bar, no account menu - but the brand, the chip and the toggle', async () => {
      const { el } = await render({ signedIn: false, items: 3 });

      expect(el.querySelector('nav[aria-label="Main"]')).toBeNull();
      expect(el.querySelector('nav[aria-label="Quick navigation"]')).toBeNull();
      expect(el.querySelector('app-account-menu')).toBeNull();
      expect(el.querySelector('header')?.textContent).toContain('Maia Bank');
      expect(el.querySelector('app-theme-toggle button')).not.toBeNull();
    });
  });

  describe('signed in', () => {
    it('shows the account menu of the customer in the top bar', async () => {
      const { el } = await render();
      expect(el.querySelector('header app-account-menu')?.textContent).toContain('Maria Silva');
    });

    it('Sign out asks for confirmation, and only the confirmation signs out', async () => {
      const { fixture, el, session } = await render();
      el.querySelector<HTMLButtonElement>('app-account-menu button[aria-haspopup]')!.click();
      await fixture.whenStable();
      Array.from(el.querySelectorAll<HTMLButtonElement>('#account-menu-panel button')).find((b) => b.textContent?.includes('Sign out'))!.click();
      await fixture.whenStable();

      const dialog = el.querySelector('[role="dialog"]')!;
      expect(dialog.textContent).toContain('Sign out of Maia Bank?');
      expect(session.logoutCalls).toBe(0);

      Array.from(dialog.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes('Sign out'))!.click();
      expect(session.logoutCalls).toBe(1);
    });

    it('Cancel in the sign-out dialog keeps the session', async () => {
      const { fixture, el, session } = await render();
      el.querySelector<HTMLButtonElement>('app-account-menu button[aria-haspopup]')!.click();
      await fixture.whenStable();
      Array.from(el.querySelectorAll<HTMLButtonElement>('#account-menu-panel button')).find((b) => b.textContent?.includes('Sign out'))!.click();
      await fixture.whenStable();

      Array.from(el.querySelectorAll<HTMLButtonElement>('[role="dialog"] button')).find((b) => b.textContent?.includes('Cancel'))!.click();
      await fixture.whenStable();

      expect(el.querySelector('[role="dialog"]')).toBeNull();
      expect(session.logoutCalls).toBe(0);
    });

    it('a session that expired shows the "session expired" dialog, and "Sign in again" starts a new login', async () => {
      const { fixture, el, session } = await render();
      expect(el.querySelector('[role="dialog"]')).toBeNull();

      session.markExpired();
      await fixture.whenStable();

      const dialog = el.querySelector('[role="dialog"]')!;
      expect(dialog.textContent).toContain('Your session expired');
      Array.from(dialog.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes('Sign in again'))!.click();
      expect(session.loginCalls).toHaveLength(1);
    });
  });
});

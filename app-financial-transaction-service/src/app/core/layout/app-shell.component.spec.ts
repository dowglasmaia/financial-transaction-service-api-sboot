import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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
  async function render(options: { items?: number; demoBanner?: boolean } = {}) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routesWith(options.items ?? 1)),
        { provide: APP_CONFIG, useValue: { ...DEFAULT_APP_CONFIG, demoBanner: options.demoBanner ?? true } },
      ],
    });
    const fixture = TestBed.createComponent(AppShellComponent);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, el };
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
});

import { TestBed } from '@angular/core/testing';
import { AccountMenuComponent } from './account-menu.component';

describe('AccountMenuComponent', () => {
  async function render(account: { agency: string; number: string } | null = null) {
    const fixture = TestBed.createComponent(AccountMenuComponent);
    fixture.componentRef.setInput('user', { name: 'Maria Silva', email: 'maria@example.test', picture: '' });
    fixture.componentRef.setInput('account', account);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const toggle = () => el.querySelector<HTMLButtonElement>('button[aria-haspopup]')!;
    return { fixture, el, toggle };
  }

  it('shows the initials and starts closed', async () => {
    const { el, toggle } = await render();
    expect(el.textContent).toContain('MS');
    expect(toggle().getAttribute('aria-expanded')).toBe('false');
    expect(el.querySelector('#account-menu-panel')).toBeNull();
  });

  it('opens a panel with name, e-mail and the account, and a Sign out action', async () => {
    const { fixture, el, toggle } = await render({ agency: '0211', number: '2022' });

    toggle().click();
    await fixture.whenStable();

    const panel = el.querySelector('#account-menu-panel')!;
    expect(toggle().getAttribute('aria-expanded')).toBe('true');
    expect(panel.textContent).toContain('Maria Silva');
    expect(panel.textContent).toContain('maria@example.test');
    expect(panel.textContent).toContain('Agency 0211 · Account 2022');
    expect(panel.textContent).toContain('Sign out');
  });

  it('Sign out closes the panel and asks the shell to confirm', async () => {
    const { fixture, el, toggle } = await render();
    const emitted: unknown[] = [];
    fixture.componentInstance.signOut.subscribe(() => emitted.push(true));
    toggle().click();
    await fixture.whenStable();

    Array.from(el.querySelectorAll<HTMLButtonElement>('#account-menu-panel button')).find((b) => b.textContent?.includes('Sign out'))!.click();
    await fixture.whenStable();

    expect(emitted).toHaveLength(1);
    expect(el.querySelector('#account-menu-panel')).toBeNull();
  });

  it('closes on Escape and on a click outside', async () => {
    const { fixture, el, toggle } = await render();
    toggle().click();
    await fixture.whenStable();
    el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await fixture.whenStable();
    expect(el.querySelector('#account-menu-panel')).toBeNull();

    toggle().click();
    await fixture.whenStable();
    document.body.click();
    await fixture.whenStable();
    expect(el.querySelector('#account-menu-panel')).toBeNull();
  });
});

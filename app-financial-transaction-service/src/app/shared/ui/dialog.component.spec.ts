import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DialogComponent } from './dialog.component';

@Component({
  imports: [DialogComponent],
  template: `
    <button id="opener" type="button" (click)="open.set(true)">Open</button>
    <app-dialog [(open)]="open" title="Sign out of Maia Bank?" description="You will need to sign in again.">
      <button id="first" type="button">Cancel</button>
      <button id="last" type="button">Sign out</button>
    </app-dialog>
  `,
})
class HostComponent {
  readonly open = signal(false);
}

describe('DialogComponent', () => {
  async function render() {
    const fixture = TestBed.createComponent(HostComponent);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const dialog = () => el.querySelector<HTMLElement>('[role="dialog"]');
    const openIt = async () => {
      el.querySelector<HTMLButtonElement>('#opener')!.focus();
      el.querySelector<HTMLButtonElement>('#opener')!.click();
      await fixture.whenStable();
    };
    return { fixture, el, dialog, openIt };
  }

  it('renders nothing while closed', async () => {
    const { dialog } = await render();
    expect(dialog()).toBeNull();
  });

  it('is a modal dialog named by its title and described by its description', async () => {
    const { el, dialog, openIt } = await render();
    await openIt();
    const node = dialog()!;
    expect(node.getAttribute('aria-modal')).toBe('true');
    const title = el.querySelector(`#${node.getAttribute('aria-labelledby')}`);
    const description = el.querySelector(`#${node.getAttribute('aria-describedby')}`);
    expect(title?.textContent).toContain('Sign out of Maia Bank?');
    expect(description?.textContent).toContain('You will need to sign in again.');
  });

  it('moves focus inside when it opens', async () => {
    const { el, openIt } = await render();
    await openIt();
    expect(el.querySelector('[role="dialog"]')!.contains(document.activeElement)).toBe(true);
  });

  it('closes on Escape and gives the focus back to the opener', async () => {
    const { fixture, el, dialog, openIt } = await render();
    await openIt();
    dialog()!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    await fixture.whenStable();
    expect(dialog()).toBeNull();
    expect(document.activeElement).toBe(el.querySelector('#opener'));
  });

  it('traps Tab: from the last control it wraps to the first', async () => {
    const { fixture, el, dialog, openIt } = await render();
    await openIt();
    const first = el.querySelector<HTMLElement>('#first')!;
    const last = el.querySelector<HTMLElement>('#last')!;
    last.focus();
    const event = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true });
    dialog()!.dispatchEvent(event);
    await fixture.whenStable();
    expect(event.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(first);
  });

  it('traps Shift+Tab: from the first control it wraps to the last', async () => {
    const { fixture, el, dialog, openIt } = await render();
    await openIt();
    const first = el.querySelector<HTMLElement>('#first')!;
    const last = el.querySelector<HTMLElement>('#last')!;
    first.focus();
    const event = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true });
    dialog()!.dispatchEvent(event);
    await fixture.whenStable();
    expect(document.activeElement).toBe(last);
  });

  it('closes when the backdrop is clicked', async () => {
    const { fixture, el, dialog, openIt } = await render();
    await openIt();
    el.querySelector<HTMLElement>('[data-dialog-backdrop]')!.click();
    await fixture.whenStable();
    expect(dialog()).toBeNull();
  });
});

import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AmountPresetsComponent } from './amount-presets.component';
import { ResultKind, ResultPanelComponent } from './result-panel.component';
import { ReviewDialogComponent } from './review-dialog.component';

@Component({
  imports: [AmountPresetsComponent, ReviewDialogComponent, ResultPanelComponent],
  template: `
    <app-amount-presets [(value)]="text" [balance]="balance" [presets]="[10, 20]" />
    <app-review-dialog [(open)]="open" title="Review thing" [lines]="[{ label: 'Amount', value: 'R$ 10.00' }]" [pending]="pending()" (confirmed)="confirmed = confirmed + 1" />
    <app-result-panel [kind]="kind()" title="T" message="M" againLabel="New thing" (again)="events.push('again')" (edit)="events.push('edit')" (retry)="events.push('retry')" />
  `,
})
class Host {
  text = signal('');
  balance = 100;
  open = signal(true);
  pending = signal(false);
  kind = signal<ResultKind>('success');
  confirmed = 0;
  events: string[] = [];
}

function setup() {
  TestBed.configureTestingModule({ providers: [provideRouter([])] });
  const fixture = TestBed.createComponent(Host);
  const el = fixture.nativeElement as HTMLElement;
  const texts = (selector: string) => [...el.querySelectorAll<HTMLElement>(selector)].map((n) => n.textContent?.trim());
  return { fixture, host: fixture.componentInstance, el, texts };
}

describe('shared money-flow pieces', () => {
  it('amount presets: the proposed amounts, the full balance and the balance line', async () => {
    const { fixture, host, el } = setup();
    await fixture.whenStable();
    const chips = [...el.querySelectorAll<HTMLButtonElement>('app-amount-presets button')];
    expect(chips.map((c) => c.textContent?.trim())).toEqual(['R$ 10', 'R$ 20', 'Use full balance']);
    expect(el.textContent).toContain('R$ 100.00');

    chips[2].click();
    await fixture.whenStable();
    expect(host.text()).toBe('100.00');
  });

  it('review dialog: shows the lines, emits confirmed, and while pending cannot be dismissed or confirmed again', async () => {
    const { fixture, host, el } = setup();
    await fixture.whenStable();
    const dialog = () => el.querySelector('[role="dialog"]')!;
    expect(dialog().textContent).toContain('R$ 10.00');

    [...dialog().querySelectorAll('button')].find((b) => b.textContent?.includes('Confirm'))!.click();
    expect(host.confirmed).toBe(1);

    host.pending.set(true);
    await fixture.whenStable();
    dialog().dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    (el.querySelector('[data-dialog-backdrop]') as HTMLElement).click();
    await fixture.whenStable();
    expect(host.open()).toBe(true);
    expect([...dialog().querySelectorAll('button')].every((b) => b.disabled || b.getAttribute('aria-disabled') === 'true')).toBe(true);
  });

  it.each([
    ['success', ['View transactions', 'New thing', 'Back to home']],
    ['business', ['Edit and try again', 'Back to home']],
    ['uncertain', ['View transactions', 'Try again', 'Back to home']],
  ] as const)('result panel (%s) offers its actions', async (kind, actions) => {
    const { fixture, host, texts } = setup();
    host.kind.set(kind);
    await fixture.whenStable();
    expect(texts('app-result-panel a, app-result-panel button')).toEqual(actions);
  });

  it('result panel: the explicit actions are emitted', async () => {
    const { fixture, host, el } = setup();
    host.kind.set('uncertain');
    await fixture.whenStable();
    [...el.querySelectorAll<HTMLButtonElement>('app-result-panel button')].find((b) => b.textContent?.includes('Try again'))!.click();
    expect(host.events).toEqual(['retry']);
  });
});

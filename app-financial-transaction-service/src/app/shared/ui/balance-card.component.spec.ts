import { TestBed } from '@angular/core/testing';
import { BalanceCardComponent } from './balance-card.component';

describe('BalanceCardComponent', () => {
  async function render(inputs: Record<string, unknown>) {
    const fixture = TestBed.createComponent(BalanceCardComponent);
    for (const [key, value] of Object.entries(inputs)) fixture.componentRef.setInput(key, value);
    await fixture.whenStable();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  }

  it('shows the amount the backend sent, formatted, and offers it in full to screen readers', async () => {
    const { el } = await render({ amount: 5000 });
    expect(el.textContent).toContain('5,000.00');
    expect(el.querySelector('.sr-only')?.textContent).toContain('R$ 5,000.00');
  });

  it('can hide and show the balance', async () => {
    const { fixture, el } = await render({ amount: 5000 });
    const toggle = el.querySelector<HTMLButtonElement>('button[aria-pressed]')!;
    expect(toggle.getAttribute('aria-label')).toBe('Hide balance');

    toggle.click();
    await fixture.whenStable();

    expect(el.textContent).not.toContain('5,000.00');
    expect(toggle.getAttribute('aria-label')).toBe('Show balance');
  });

  describe('count-up', () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('counts up to the real amount and ends exactly on it', () => {
      const fixture = TestBed.createComponent(BalanceCardComponent);
      fixture.componentRef.setInput('amount', 4250.5);
      fixture.componentRef.setInput('countUp', true);
      fixture.detectChanges();
      const visible = () => (fixture.nativeElement as HTMLElement).querySelector('[aria-hidden="true"]')?.textContent ?? '';
      expect(visible()).not.toContain('4,250.50');

      vi.advanceTimersByTime(2000);
      fixture.detectChanges();

      expect(visible()).toContain('4,250.50');
    });

    it('the screen-reader text is always the final amount, never an intermediate number', () => {
      const fixture = TestBed.createComponent(BalanceCardComponent);
      fixture.componentRef.setInput('amount', 4250.5);
      fixture.componentRef.setInput('countUp', true);
      fixture.detectChanges();

      expect((fixture.nativeElement as HTMLElement).querySelector('.sr-only')?.textContent).toContain('R$ 4,250.50');
    });
  });
});

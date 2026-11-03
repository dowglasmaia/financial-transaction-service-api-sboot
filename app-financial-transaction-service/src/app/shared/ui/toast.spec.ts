import { TestBed } from '@angular/core/testing';
import { ToastHostComponent } from './toast-host.component';
import { ToastService } from './toast.service';

describe('Toast', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  // detectChanges (not whenStable): the zoneless scheduler's own timers are faked here.
  function render() {
    const fixture = TestBed.createComponent(ToastHostComponent);
    fixture.detectChanges();
    const service = TestBed.inject(ToastService);
    return { fixture, el: fixture.nativeElement as HTMLElement, service };
  }

  it('shows a message in a polite live region', () => {
    const { fixture, el, service } = render();
    service.show('Transaction id copied');
    fixture.detectChanges();
    const region = el.querySelector('[role="status"]');
    expect(region?.getAttribute('aria-live')).toBe('polite');
    expect(region?.textContent).toContain('Transaction id copied');
  });

  it('keeps the live region mounted while empty so the next message is announced', () => {
    const { el } = render();
    expect(el.querySelector('[role="status"]')).not.toBeNull();
  });

  it('dismisses itself after the timeout', () => {
    const { fixture, el, service } = render();
    service.show('Demo action', 2400);
    fixture.detectChanges();
    vi.advanceTimersByTime(2500);
    fixture.detectChanges();
    expect(el.textContent).not.toContain('Demo action');
  });

  it('a newer message replaces the older one and restarts the timer', () => {
    const { fixture, el, service } = render();
    service.show('First', 2400);
    vi.advanceTimersByTime(2000);
    service.show('Second', 2400);
    vi.advanceTimersByTime(1000);
    fixture.detectChanges();
    expect(el.textContent).toContain('Second');
    expect(el.textContent).not.toContain('First');
  });
});

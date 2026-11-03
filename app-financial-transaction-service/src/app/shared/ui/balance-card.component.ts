import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { formatBrl } from '../util/money';

/** The hero figure: current balance on the v2 blue panel with a privacy toggle. The amount always comes from the backend. */
@Component({
  selector: 'app-balance-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section
      class="relative overflow-hidden rounded-panel border border-white/25 bg-hero p-6 text-on-primary shadow-overlay sm:p-8"
      aria-labelledby="balance-label"
    >
      <i class="ph-bold ph-bank pointer-events-none absolute -right-6 -bottom-10 text-[220px] text-white/10" aria-hidden="true"></i>
      <div class="relative flex items-start justify-between gap-4">
        <p id="balance-label" class="text-sm font-semibold text-on-primary/80">{{ label() }}</p>
        <button
          type="button"
          class="flex size-11 items-center justify-center rounded-control text-on-primary/80 hover:bg-white/10 hover:text-on-primary focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-on-primary"
          [attr.aria-pressed]="hidden()"
          [attr.aria-label]="hidden() ? 'Show balance' : 'Hide balance'"
          (click)="hidden.set(!hidden())"
        >
          <i [class]="hidden() ? 'ph-bold ph-eye-slash' : 'ph-bold ph-eye'" aria-hidden="true"></i>
        </button>
      </div>
      <p class="relative mt-1 font-display text-4xl font-semibold sm:text-5xl">
        @if (hidden()) {
          <span aria-label="Balance hidden">R$ ••••••</span>
        } @else {
          <span aria-hidden="true"><span class="mr-2 text-on-primary/75">R$</span>{{ shownDigits() }}</span>
          <span class="sr-only">{{ formatted() }}</span>
        }
      </p>
      @if (updated()) {
        <p class="relative mt-3 text-sm text-on-primary/80">{{ updated() }}</p>
      }
    </section>
  `,
})
export class BalanceCardComponent {
  readonly label = input('Current balance');
  readonly amount = input.required<number | string>();
  readonly updated = input('');
  /** Animate from zero to the amount (skipped when the user prefers reduced motion). The final value is always the real one. */
  readonly countUp = input(false);

  protected readonly hidden = signal(false);
  protected readonly formatted = computed(() => formatBrl(this.amount()));
  private readonly shown = signal<number | null>(null);
  protected readonly shownDigits = computed(() => {
    const value = this.shown();
    return (value === null ? this.formatted() : formatBrl(value)).replace('R$ ', '');
  });

  private timer: ReturnType<typeof setInterval> | undefined;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearInterval(this.timer));
    effect(() => {
      const target = Number(this.amount());
      clearInterval(this.timer);
      const reduced = typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;
      if (!this.countUp() || reduced || !Number.isFinite(target)) {
        this.shown.set(null);
        return;
      }
      const steps = 24;
      let step = 0;
      this.shown.set(0);
      this.timer = setInterval(() => {
        step++;
        this.shown.set(step >= steps ? null : target * (1 - Math.pow(1 - step / steps, 3)));
        if (step >= steps) clearInterval(this.timer);
      }, 30);
    });
  }
}

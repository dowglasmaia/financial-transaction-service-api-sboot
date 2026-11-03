import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
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
      <p class="relative mt-1 font-display text-4xl font-semibold sm:text-5xl" aria-live="polite">
        @if (hidden()) {
          <span aria-label="Balance hidden">R$ ••••••</span>
        } @else {
          <span class="mr-2 text-on-primary/75" aria-hidden="true">R$</span><span>{{ digits() }}</span>
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

  protected readonly hidden = signal(false);
  protected readonly formatted = computed(() => formatBrl(this.amount()));
  protected readonly digits = computed(() => this.formatted().replace('R$ ', ''));
}

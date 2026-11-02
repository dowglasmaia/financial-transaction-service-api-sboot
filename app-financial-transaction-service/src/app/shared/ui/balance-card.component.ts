import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { formatBrl } from '../util/money';

/** The hero figure: current balance with an optional privacy toggle. The amount always comes from the backend. */
@Component({
  selector: 'app-balance-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="rounded-card border border-line bg-surface-raised p-6 shadow-card" aria-labelledby="balance-label">
      <div class="flex items-start justify-between gap-4">
        <p id="balance-label" class="text-sm font-medium text-content-muted">{{ label() }}</p>
        <button
          type="button"
          class="flex size-11 items-center justify-center rounded-control text-content-muted hover:bg-surface-sunken hover:text-content focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-accent"
          [attr.aria-pressed]="hidden()"
          [attr.aria-label]="hidden() ? 'Show balance' : 'Hide balance'"
          (click)="hidden.set(!hidden())"
        >
          <i [class]="hidden() ? 'ph-bold ph-eye-slash' : 'ph-bold ph-eye'" aria-hidden="true"></i>
        </button>
      </div>
      <p class="mt-1 font-display text-4xl font-semibold sm:text-5xl" aria-live="polite">
        @if (hidden()) {
          <span aria-label="Balance hidden">R$ ••••••</span>
        } @else {
          <span class="text-content-muted" aria-hidden="true">R$</span> <span>{{ digits() }}</span>
          <span class="sr-only">{{ formatted() }}</span>
        }
      </p>
      @if (updated()) {
        <p class="mt-3 text-sm text-content-muted">{{ updated() }}</p>
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

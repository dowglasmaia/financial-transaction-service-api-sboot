import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { amountFeedback, formatBrl } from '../util/money';
import { ChipComponent } from './chip.component';
import { FormFieldComponent } from './form-field';
import { MoneyInputComponent } from './money-input.component';

/**
 * Amount entry shared by the money flows: proposed amounts, an optional "Use full balance", the input, the available
 * balance and early feedback ("can't exceed your balance", "greater than zero"). The feedback only helps the customer;
 * the server decides what is acceptable. `value` is the typed text; read the normalized amount with `parseAmountInput`.
 *
 * Used by Withdraw (SL-03) and reused by Pix (SL-06) and Deposit/Recharge (SL-04).
 */
@Component({
  selector: 'app-amount-presets',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChipComponent, FormFieldComponent, MoneyInputComponent],
  template: `
    <div class="grid gap-4">
      <div class="flex flex-wrap items-center gap-2" role="group" [attr.aria-label]="'Proposed amounts'">
        @for (preset of presets(); track preset) {
          <app-chip [selected]="selectedPreset() === preset" (toggled)="value.set(preset + '.00')">R$ {{ preset }}</app-chip>
        }
        @if (fullBalance() && balance() !== null) {
          <app-chip [selected]="false" (toggled)="useFullBalance()">Use full balance</app-chip>
        }
      </div>

      <app-form-field [label]="label()" [error]="feedback().message ?? ''">
        <app-money-input [(value)]="value" [disabled]="disabled()" />
      </app-form-field>

      @if (balance() !== null) {
        <p class="text-sm text-content-muted">Available balance <b class="text-content">{{ formattedBalance() }}</b></p>
      }
    </div>
  `,
})
export class AmountPresetsComponent {
  /** The typed text (two-way). */
  readonly value = model('');
  readonly label = input('Amount');
  readonly presets = input<readonly number[]>([50, 100, 200, 500]);
  /** The balance reported by the API; null while unknown. */
  readonly balance = input<number | null>(null);
  readonly fullBalance = input(true);
  readonly disabled = input(false);

  protected readonly feedback = computed(() => amountFeedback(this.value(), this.balance()));
  protected readonly formattedBalance = computed(() => formatBrl(this.balance() ?? 0));
  protected readonly selectedPreset = computed(() => {
    const amount = this.feedback().amount;
    return amount === null ? null : (this.presets().find((preset) => Number(amount) === preset) ?? null);
  });

  protected useFullBalance(): void {
    const balance = this.balance();
    if (balance !== null) this.value.set(balance.toFixed(2));
  }
}

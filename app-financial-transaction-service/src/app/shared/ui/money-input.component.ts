import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { FieldControlDirective } from './form-field';
import { parseAmountInput } from '../util/money';

/**
 * Amount input in Brazilian reais. `value` is what the user typed; `amount` is the normalized two-decimal string
 * (or null when invalid). It only gives early feedback — the backend decides what is acceptable.
 */
@Component({
  selector: 'app-money-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FieldControlDirective],
  template: `
    <div class="relative">
      <span class="pointer-events-none absolute inset-y-0 left-3.5 flex items-center text-content-muted" aria-hidden="true">R$</span>
      <input
        appFieldControl
        class="pl-11"
        type="text"
        inputmode="decimal"
        autocomplete="off"
        [placeholder]="placeholder()"
        [disabled]="disabled()"
        [value]="value()"
        (input)="value.set(read($event))"
        (blur)="normalize()"
      />
    </div>
  `,
})
export class MoneyInputComponent {
  readonly value = model('');
  readonly placeholder = input('0.00');
  readonly disabled = input(false);

  /** Normalized amount (`"1234.50"`) or null while empty/invalid. */
  readonly amount = computed(() => parseAmountInput(this.value()));

  protected read(event: Event): string {
    return (event.target as HTMLInputElement).value.replace(/[^\d.,]/g, '');
  }

  protected normalize(): void {
    const normalized = this.amount();
    if (normalized) this.value.set(normalized);
  }
}

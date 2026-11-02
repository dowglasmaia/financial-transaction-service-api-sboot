import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { formatBrl } from '../util/money';
import { TRANSACTION_TYPES, TransactionType } from './transaction-types';

/**
 * One statement row. Direction is shown by a coloured rail, an icon, the type label and the sign of the amount —
 * never by colour alone.
 */
@Component({
  selector: 'app-ledger-row',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe],
  template: `
    <div class="flex items-center gap-4 rounded-card border border-line bg-surface-raised py-3 pr-4 pl-0 shadow-card">
      <span class="h-10 w-[3px] shrink-0 rounded-r" [class]="info().direction === 'in' ? 'bg-income' : 'bg-expense'" aria-hidden="true"></span>
      <span class="flex size-10 shrink-0 items-center justify-center rounded-full bg-surface-sunken" [class]="tone()">
        <i [class]="info().icon" aria-hidden="true"></i>
      </span>
      <div class="min-w-0 flex-1">
        <p class="truncate font-semibold">{{ info().label }}</p>
        <p class="text-sm text-content-muted">{{ date() | date: 'MMM d, y, h:mm a' }}</p>
      </div>
      <p class="font-display text-[17px] font-semibold" [class]="tone()">
        <span class="sr-only">{{ info().direction === 'in' ? 'Money in' : 'Money out' }} </span>{{ signed() }}
      </p>
    </div>
  `,
})
export class LedgerRowComponent {
  readonly type = input.required<TransactionType>();
  readonly amount = input.required<number | string>();
  readonly date = input.required<string | Date>();

  protected readonly info = computed(() => TRANSACTION_TYPES[this.type()]);
  protected readonly tone = computed(() => (this.info().direction === 'in' ? 'text-income' : 'text-expense'));
  protected readonly signed = computed(() => `${this.info().direction === 'in' ? '+' : '−'} ${formatBrl(Math.abs(Number(this.amount())))}`);
}

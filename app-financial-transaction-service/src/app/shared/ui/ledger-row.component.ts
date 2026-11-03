import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { formatBrl } from '../util/money';
import { IconTileComponent } from './icon-tile.component';
import { TRANSACTION_TYPES, TransactionType } from './transaction-types';

/**
 * One statement row. Direction is shown by the icon tile of the kind, the type label, the sign of the amount and
 * screen-reader text — never by colour alone.
 */
@Component({
  selector: 'app-ledger-row',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, IconTileComponent],
  template: `
    <div class="flex items-center gap-3.5 rounded-card border border-line bg-glass px-4 py-3 shadow-card">
      <app-icon-tile [icon]="info().icon" [tone]="info().tile" />
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

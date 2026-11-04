import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { map } from 'rxjs';
import { AlertComponent } from '../../../shared/ui/alert.component';
import { ButtonDirective } from '../../../shared/ui/button.directive';
import { ChipComponent } from '../../../shared/ui/chip.component';
import { EmptyStateComponent } from '../../../shared/ui/empty-state.component';
import { KpiTileComponent } from '../../../shared/ui/kpi-tile.component';
import { LedgerRowComponent } from '../../../shared/ui/ledger-row.component';
import { SkeletonComponent } from '../../../shared/ui/skeleton.component';
import { TRANSACTION_TYPES, TransactionType } from '../../../shared/ui/transaction-types';
import { formatBrl } from '../../../shared/util/money';
import { StatementStore } from '../data-access/ledger.stores';
import { DEFAULT_QUERY, PAGE_SIZE, PeriodKey, StatementFilters, queryFromParams, queryToParams } from '../data-access/statement-query';

const PERIODS: readonly { key: PeriodKey; label: string }[] = [
  { key: '7', label: '7 days' },
  { key: '30', label: '30 days' },
  { key: '90', label: '90 days' },
  { key: 'custom', label: 'Custom' },
];

const TYPE_CHIPS: readonly { type: TransactionType | null; label: string }[] = [
  { type: null, label: 'All' },
  { type: 'DEPOSIT', label: 'Deposit' },
  { type: 'PIX', label: 'Pix' },
  { type: 'PHONE_RECHARGE', label: 'Recharge' },
  { type: 'WITHDRAW', label: 'Withdrawal' },
];

/**
 * Statement: the filters live in the URL (shareable, the back button works) and every figure comes from the backend —
 * totals cover the whole period, not just the visible page.
 */
@Component({
  selector: 'app-statement-page',
  imports: [AlertComponent, ButtonDirective, ChipComponent, DatePipe, EmptyStateComponent, KpiTileComponent, LedgerRowComponent, RouterLink, SkeletonComponent],
  templateUrl: './statement-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatementPage {
  protected readonly store = inject(StatementStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly periods = PERIODS;
  protected readonly typeChips = TYPE_CHIPS;
  protected readonly types = TRANSACTION_TYPES;
  protected readonly format = formatBrl;

  protected readonly filters = toSignal(
    this.route.queryParamMap.pipe(map((params) => queryFromParams(Object.fromEntries(params.keys.map((key) => [key, params.get(key) ?? undefined]))))),
    { initialValue: DEFAULT_QUERY },
  );

  /** The date inputs show while "Custom" is chosen, even before a complete range exists. */
  protected readonly customOpen = signal(false);
  protected readonly customFrom = signal('');
  protected readonly customTo = signal('');
  protected readonly customInvalid = computed(() => !!this.customFrom() && !!this.customTo() && this.customFrom() > this.customTo());

  protected readonly firstShown = computed(() => (this.store.total() === 0 ? 0 : this.filters().page * PAGE_SIZE + 1));
  protected readonly lastShown = computed(() => this.filters().page * PAGE_SIZE + this.store.rows().length);
  protected readonly hasPrevious = computed(() => this.filters().page > 0);
  protected readonly hasNext = computed(() => this.lastShown() < this.store.total());
  /** Sort control value, e.g. `date-desc`. */
  protected readonly sortValue = computed(() => `${this.filters().sort}-${this.filters().order}`);

  constructor() {
    effect(() => {
      const filters = this.filters();
      this.customFrom.set(filters.from ?? '');
      this.customTo.set(filters.to ?? '');
      this.customOpen.set(filters.period === 'custom');
      this.store.load(filters);
    });
  }

  protected setPeriod(period: PeriodKey): void {
    if (period === 'custom') {
      this.customOpen.set(true);
      return;
    }
    this.customOpen.set(false);
    this.go({ period, from: null, to: null });
  }

  protected applyCustom(): void {
    if (!this.customFrom() || !this.customTo() || this.customInvalid()) return;
    this.go({ period: 'custom', from: this.customFrom(), to: this.customTo() });
  }

  protected setType(type: TransactionType | null): void {
    this.go({ type });
  }

  protected setSort(value: string): void {
    const [sort, order] = value.split('-');
    this.go({ sort: sort === 'amount' ? 'amount' : 'date', order: order === 'asc' ? 'asc' : 'desc' });
  }

  protected goToPage(delta: number): void {
    this.go({ page: Math.max(0, this.filters().page + delta) }, false);
  }

  protected retry(): void {
    this.store.load(this.filters());
  }

  protected inputValue(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  private go(patch: Partial<StatementFilters>, resetPage = true): void {
    const next = { ...this.filters(), ...(resetPage ? { page: 0 } : {}), ...patch };
    // `custom` without a complete range is only a draft: keep the URL on the current range until "Apply".
    if (next.period === 'custom' && !(next.from && next.to)) return;
    void this.router.navigate([], { relativeTo: this.route, queryParams: queryToParams(next) });
  }
}

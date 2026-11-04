import { Injectable, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { PAGE_SIZE, StatementFilters, rangeOf } from './statement-query';
import { PeriodSummary, StatementApi, StatementResponse, TransactionRow } from './statement.api';

export type LoadState = 'idle' | 'loading' | 'ready' | 'error';

export interface Totals {
  readonly incoming: number;
  readonly outgoing: number;
  readonly balance: number;
}

/** One page of the statement. Totals and counts are the backend's; the store never adds anything up. */
@Injectable({ providedIn: 'root' })
export class StatementStore {
  private readonly api = inject(StatementApi);

  readonly state = signal<LoadState>('idle');
  readonly error = signal<ApiError | null>(null);
  private readonly data = signal<StatementResponse | null>(null);

  readonly rows = computed(() => this.data()?.transaction ?? []);
  readonly total = computed(() => this.data()?.totalElements ?? 0);
  readonly totals = computed<Totals | null>(() => {
    const page = this.data();
    return page ? { incoming: page.sumIncoming ?? 0, outgoing: page.sumOutgoing ?? 0, balance: page.balance ?? 0 } : null;
  });
  readonly isEmpty = computed(() => this.state() === 'ready' && this.rows().length === 0);

  load(filters: StatementFilters): void {
    this.state.set('loading');
    this.error.set(null);
    const { from, to } = rangeOf(filters);
    this.cancelPrevious();
    this.pending = this.api
      .list({ from, to, type: filters.type, sort: filters.sort, order: filters.order, page: filters.page, size: PAGE_SIZE })
      .subscribe({ next: (page) => this.done(page), error: (error: ApiError) => this.fail(error) });
  }

  clear(): void {
    this.cancelPrevious();
    this.data.set(null);
    this.error.set(null);
    this.state.set('idle');
  }

  private pending: { unsubscribe(): void } | null = null;

  private cancelPrevious(): void {
    this.pending?.unsubscribe();
    this.pending = null;
  }

  private done(page: StatementResponse): void {
    this.data.set(page);
    this.state.set('ready');
  }

  private fail(error: ApiError): void {
    this.data.set(null);
    this.error.set(error);
    this.state.set('error');
  }
}

export type DetailState = LoadState | 'not-found';

/** One transaction by id. A foreign or unknown id (404) and a malformed one (400) are the same "not found" for the customer. */
@Injectable({ providedIn: 'root' })
export class TransactionDetailStore {
  private readonly api = inject(StatementApi);
  private pending: { unsubscribe(): void } | null = null;

  readonly state = signal<DetailState>('idle');
  readonly row = signal<TransactionRow | null>(null);
  readonly error = signal<ApiError | null>(null);

  load(id: string): void {
    this.pending?.unsubscribe();
    this.state.set('loading');
    this.row.set(null);
    this.error.set(null);
    this.pending = this.api.get(id).subscribe({
      next: (row) => {
        this.row.set(row);
        this.state.set('ready');
      },
      error: (error: ApiError) => {
        this.error.set(error);
        this.state.set(error.status === 404 || error.status === 400 ? 'not-found' : 'error');
      },
    });
  }

  clear(): void {
    this.pending?.unsubscribe();
    this.row.set(null);
    this.error.set(null);
    this.state.set('idle');
  }
}

export const RECENT_ROWS = 5;

/** Home: the period summary (totals, balance, daily series) and the most recent rows of the same period. */
@Injectable({ providedIn: 'root' })
export class SummaryStore {
  private readonly api = inject(StatementApi);
  private pending: { unsubscribe(): void } | null = null;

  readonly state = signal<LoadState>('idle');
  readonly error = signal<ApiError | null>(null);
  readonly summary = signal<PeriodSummary | null>(null);
  readonly recent = signal<TransactionRow[]>([]);

  readonly series = computed(() => (this.summary()?.series ?? []).map((d) => ({ date: d.date ?? '', incoming: d.incoming ?? 0, outgoing: d.outgoing ?? 0 })));
  readonly hasActivity = computed(() => this.recent().length > 0 || this.series().some((d) => (d.incoming ?? 0) > 0 || (d.outgoing ?? 0) > 0));

  load(from: string, to: string): void {
    this.pending?.unsubscribe();
    this.state.set('loading');
    this.error.set(null);
    this.pending = forkJoin({
      summary: this.api.summary(from, to),
      recent: this.api.list({ from, to, type: null, sort: 'date', order: 'desc', page: 0, size: RECENT_ROWS }),
    }).subscribe({
      next: ({ summary, recent }) => {
        this.summary.set(summary);
        this.recent.set(recent.transaction ?? []);
        this.state.set('ready');
      },
      error: (error: ApiError) => {
        this.error.set(error);
        this.state.set('error');
      },
    });
  }

  clear(): void {
    this.pending?.unsubscribe();
    this.summary.set(null);
    this.recent.set([]);
    this.error.set(null);
    this.state.set('idle');
  }
}

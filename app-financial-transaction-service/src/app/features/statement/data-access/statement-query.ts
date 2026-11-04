import type { TransactionType } from '../../../shared/ui/transaction-types';

export type PeriodKey = '7' | '30' | '90' | 'custom';
export type SortKey = 'date' | 'amount';
export type OrderKey = 'asc' | 'desc';

/** What the Statement screen shows, and exactly what lives in the URL (shareable; the back button works). */
export interface StatementFilters {
  readonly period: PeriodKey;
  /** Only for `custom`: inclusive ISO days. */
  readonly from: string | null;
  readonly to: string | null;
  readonly type: TransactionType | null;
  readonly sort: SortKey;
  readonly order: OrderKey;
  readonly page: number;
}

export const PAGE_SIZE = 10;

export const DEFAULT_QUERY: StatementFilters = { period: '30', from: null, to: null, type: null, sort: 'date', order: 'desc', page: 0 };

const TYPES: readonly TransactionType[] = ['DEPOSIT', 'PIX', 'PHONE_RECHARGE', 'WITHDRAW'];
const ISO_DAY = /^\d{4}-\d{2}-\d{2}$/;

/** yyyy-MM-dd of the local calendar day (never through UTC, which could shift the day). */
export function isoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** The last `days` days including today. */
export function periodRange(days: number, today: Date = new Date()): { from: string; to: string } {
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate() - (days - 1));
  return { from: isoDate(start), to: isoDate(today) };
}

/** The inclusive range the filters stand for. */
export function rangeOf(filters: StatementFilters, today: Date = new Date()): { from: string; to: string } {
  if (filters.period === 'custom' && filters.from && filters.to) return { from: filters.from, to: filters.to };
  return periodRange(Number(filters.period === 'custom' ? 30 : filters.period), today);
}

export function queryToParams(filters: StatementFilters): Record<string, string> {
  const params: Record<string, string> = {};
  if (filters.period !== DEFAULT_QUERY.period) params['period'] = filters.period;
  if (filters.period === 'custom' && filters.from && filters.to) {
    params['from'] = filters.from;
    params['to'] = filters.to;
  }
  if (filters.type) params['type'] = filters.type;
  if (filters.sort !== DEFAULT_QUERY.sort) params['sort'] = filters.sort;
  if (filters.order !== DEFAULT_QUERY.order) params['order'] = filters.order;
  if (filters.page > 0) params['page'] = String(filters.page);
  return params;
}

function validDay(value: string | undefined): value is string {
  return !!value && ISO_DAY.test(value) && !Number.isNaN(Date.parse(value));
}

/** Reads the URL; anything invalid falls back to the defaults, so a hand-edited link never breaks the screen. */
export function queryFromParams(params: Readonly<Record<string, string | undefined>>): StatementFilters {
  const custom = params['period'] === 'custom';
  if (custom && !(validDay(params['from']) && validDay(params['to']) && params['from'] <= params['to'])) return DEFAULT_QUERY;
  const period: PeriodKey = custom ? 'custom' : params['period'] === '7' || params['period'] === '90' ? params['period'] : '30';
  const type = TYPES.find((candidate) => candidate === params['type']) ?? null;
  const page = Number(params['page']);
  return {
    period,
    from: custom ? (params['from'] ?? null) : null,
    to: custom ? (params['to'] ?? null) : null,
    type,
    sort: params['sort'] === 'amount' ? 'amount' : 'date',
    order: params['order'] === 'asc' ? 'asc' : 'desc',
    page: Number.isInteger(page) && page > 0 ? page : 0,
  };
}

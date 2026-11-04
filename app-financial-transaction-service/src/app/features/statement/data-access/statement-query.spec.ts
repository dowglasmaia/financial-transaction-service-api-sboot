import { DEFAULT_QUERY, isoDate, periodRange, queryFromParams, queryToParams } from './statement-query';

describe('statement query', () => {
  it('formats a local date as yyyy-MM-dd without shifting the day', () => {
    expect(isoDate(new Date(2026, 2, 5, 23, 59))).toBe('2026-03-05');
  });

  it('turns a preset into a range ending today (7 days = today and the 6 before)', () => {
    expect(periodRange(7, new Date(2026, 2, 11))).toEqual({ from: '2026-03-05', to: '2026-03-11' });
    expect(periodRange(30, new Date(2026, 2, 11))).toEqual({ from: '2026-02-10', to: '2026-03-11' });
  });

  it('starts with the last 30 days, every type, newest first, first page', () => {
    expect(DEFAULT_QUERY).toEqual({ period: '30', from: null, to: null, type: null, sort: 'date', order: 'desc', page: 0 });
  });

  it('keeps only what differs from the defaults in the URL (shareable, short)', () => {
    expect(queryToParams(DEFAULT_QUERY)).toEqual({});
    expect(queryToParams({ ...DEFAULT_QUERY, period: '7', type: 'PIX', sort: 'amount', order: 'asc', page: 2 })).toEqual({
      period: '7', type: 'PIX', sort: 'amount', order: 'asc', page: '2',
    });
    expect(queryToParams({ ...DEFAULT_QUERY, period: 'custom', from: '2026-03-11', to: '2026-03-11' })).toEqual({
      period: 'custom', from: '2026-03-11', to: '2026-03-11',
    });
  });

  it('reads the query back from the URL and falls back to the defaults for anything invalid', () => {
    expect(queryFromParams({ period: '90', type: 'WITHDRAW', sort: 'amount', order: 'asc', page: '3' })).toEqual({
      ...DEFAULT_QUERY, period: '90', type: 'WITHDRAW', sort: 'amount', order: 'asc', page: 3,
    });
    expect(queryFromParams({ period: 'x', type: 'LOAN', sort: 'name', order: 'up', page: '-4' })).toEqual(DEFAULT_QUERY);
    expect(queryFromParams({ period: 'custom', from: '2026-03-11', to: 'nonsense' })).toEqual(DEFAULT_QUERY);
    expect(queryFromParams({ period: 'custom', from: '2026-03-12', to: '2026-03-10' })).toEqual(DEFAULT_QUERY);
  });

  it('accepts a custom single day', () => {
    expect(queryFromParams({ period: 'custom', from: '2026-03-11', to: '2026-03-11' })).toEqual({
      ...DEFAULT_QUERY, period: 'custom', from: '2026-03-11', to: '2026-03-11',
    });
  });
});

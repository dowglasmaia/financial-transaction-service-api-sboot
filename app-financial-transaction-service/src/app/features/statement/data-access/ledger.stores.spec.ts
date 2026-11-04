import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../../../core/http/api-base-url.token';
import { DEFAULT_QUERY } from './statement-query';
import { StatementStore, SummaryStore, TransactionDetailStore } from './ledger.stores';

function setup() {
  TestBed.configureTestingModule({
    providers: [{ provide: API_BASE_URL, useValue: '/api/v1' }, provideHttpClient(), provideHttpClientTesting()],
  });
  return { controller: TestBed.inject(HttpTestingController) };
}

const PAGE = {
  transaction: [{ id: 'a', transactionType: 'PIX', amount: 12.5, dateHour: '2026-03-11T12:00:00Z' }],
  sumIncoming: 100,
  sumOutgoing: 35,
  balance: 4900,
  page: 0,
  size: 10,
  totalElements: 1,
};

describe('StatementStore', () => {
  it('loads one page of the period and keeps the backend totals untouched', () => {
    const { controller } = setup();
    const store = TestBed.inject(StatementStore);
    store.load({ ...DEFAULT_QUERY, period: 'custom', from: '2026-03-11', to: '2026-03-11' });
    expect(store.state()).toBe('loading');

    controller.expectOne((r) => r.url === '/api/v1/transactions').flush(PAGE);

    expect(store.state()).toBe('ready');
    expect(store.rows()).toHaveLength(1);
    expect(store.totals()).toEqual({ incoming: 100, outgoing: 35, balance: 4900 });
    expect(store.total()).toBe(1);
    expect(store.isEmpty()).toBe(false);
  });

  it('knows an empty period', () => {
    const { controller } = setup();
    const store = TestBed.inject(StatementStore);
    store.load(DEFAULT_QUERY);
    controller.expectOne((r) => r.url === '/api/v1/transactions').flush({ ...PAGE, transaction: [], totalElements: 0 });
    expect(store.isEmpty()).toBe(true);
  });

  it('keeps the error with its request id so the screen can offer a retry', () => {
    const { controller } = setup();
    const store = TestBed.inject(StatementStore);
    store.load(DEFAULT_QUERY);
    controller.expectOne((r) => r.url === '/api/v1/transactions').flush({ code: 'UNPROCESSABLE_ENTITY', message: 'bad' }, { status: 422, statusText: 'x' });
    expect(store.state()).toBe('error');
    expect(store.error()?.status).toBe(422);
  });

  it('ignores a late answer of a superseded request', () => {
    const { controller } = setup();
    const store = TestBed.inject(StatementStore);
    store.load(DEFAULT_QUERY);
    store.load({ ...DEFAULT_QUERY, type: 'PIX' });
    const [first, second] = controller.match((r) => r.url === '/api/v1/transactions');
    expect(first.cancelled).toBe(true);
    second.flush({ ...PAGE, totalElements: 7 });
    expect(store.total()).toBe(7);
  });
});

describe('TransactionDetailStore', () => {
  it('loads one transaction by id', () => {
    const { controller } = setup();
    const store = TestBed.inject(TransactionDetailStore);
    store.load('abc');
    controller.expectOne('/api/v1/transactions/abc').flush(PAGE.transaction[0]);
    expect(store.state()).toBe('ready');
    expect(store.row()?.id).toBe('a');
  });

  it('shows not found for an unknown or foreign id (404) and a malformed one (400)', () => {
    const { controller } = setup();
    const store = TestBed.inject(TransactionDetailStore);
    store.load('abc');
    controller.expectOne('/api/v1/transactions/abc').flush({ code: 'NOT_FOUND', message: 'Transaction not found' }, { status: 404, statusText: 'x' });
    expect(store.state()).toBe('not-found');
    store.load('nope');
    controller.expectOne('/api/v1/transactions/nope').flush({ code: 'BAD_REQUEST', message: 'x' }, { status: 400, statusText: 'x' });
    expect(store.state()).toBe('not-found');
  });

  it('treats any other failure as an error to retry', () => {
    const { controller } = setup();
    const store = TestBed.inject(TransactionDetailStore);
    store.load('abc');
    controller.expectOne('/api/v1/transactions/abc').flush({}, { status: 500, statusText: 'x' });
    expect(store.state()).toBe('error');
  });
});

describe('SummaryStore', () => {
  it('loads the summary and the 5 most recent rows of the same period', () => {
    const { controller } = setup();
    const store = SummaryStoreFactory();
    store.load('2026-03-05', '2026-03-11');
    controller.expectOne((r) => r.url === '/api/v1/transactions/summary').flush({ sumIncoming: 100, sumOutgoing: 35, balance: 4900, series: [{ date: '2026-03-11', incoming: 100, outgoing: 35 }] });
    const recent = controller.expectOne((r) => r.url === '/api/v1/transactions');
    expect(recent.request.params.get('size')).toBe('5');
    expect(recent.request.params.get('sort')).toBe('date');
    recent.flush(PAGE);

    expect(store.state()).toBe('ready');
    expect(store.summary()?.balance).toBe(4900);
    expect(store.recent()).toHaveLength(1);
    expect(store.hasActivity()).toBe(true);
  });

  it('has no activity when the period has no money moved', () => {
    const { controller } = setup();
    const store = SummaryStoreFactory();
    store.load('2026-03-05', '2026-03-11');
    controller.expectOne((r) => r.url === '/api/v1/transactions/summary').flush({ sumIncoming: 0, sumOutgoing: 0, balance: 5000, series: [] });
    controller.expectOne((r) => r.url === '/api/v1/transactions').flush({ ...PAGE, transaction: [], totalElements: 0 });
    expect(store.hasActivity()).toBe(false);
  });
});

function SummaryStoreFactory(): SummaryStore {
  return TestBed.inject(SummaryStore);
}

import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { StatementApi, StatementResponse, TransactionRow } from '../data-access/statement.api';
import { DetailPage } from './detail-page';
import { StatementPage } from './statement-page';

const ROW: TransactionRow = { id: '6c6fa8c9-743a-4c79-9cf6-e1d5e25ad4a5', transactionType: 'PIX', amount: 12.5, dateHour: '2026-03-11T12:00:00Z' };
const PAGE: StatementResponse = { transaction: [ROW], sumIncoming: 100, sumOutgoing: 35, balance: 4900, page: 0, size: 10, totalElements: 31 };

function statementSetup(query: Record<string, string>, list: () => unknown = () => of(PAGE)) {
  const params = new BehaviorSubject(convertToParamMap(query));
  const api = { list: vi.fn(list) };
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: StatementApi, useValue: api }, { provide: ActivatedRoute, useValue: { queryParamMap: params } }],
  });
  const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  const fixture = TestBed.createComponent(StatementPage);
  return { fixture, api, navigate, el: fixture.nativeElement as HTMLElement };
}

describe('StatementPage', () => {
  it('asks the backend for the period in the URL and shows its totals and counts untouched', async () => {
    const { fixture, api, el } = statementSetup({ period: 'custom', from: '2026-03-11', to: '2026-03-11', type: 'PIX', sort: 'amount', order: 'asc', page: '1' });
    await fixture.whenStable();

    expect(api.list).toHaveBeenCalledWith({ from: '2026-03-11', to: '2026-03-11', type: 'PIX', sort: 'amount', order: 'asc', page: 1, size: 10 });
    expect(el.textContent).toContain('R$ 100.00');
    expect(el.textContent).toContain('R$ 35.00');
    expect(el.textContent).toContain('R$ 4,900.00');
    expect(el.textContent).toContain('Showing 11–11 of 31');
  });

  it('puts a chosen type into the URL and goes back to the first page', async () => {
    const { fixture, navigate, el } = statementSetup({ page: '2' });
    await fixture.whenStable();

    [...el.querySelectorAll<HTMLButtonElement>('[aria-label="Type"] button')].find((b) => b.textContent?.includes('Withdrawal'))!.click();

    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { type: 'WITHDRAW' } }));
  });

  it('puts the sort choice and the next page into the URL', async () => {
    const { fixture, navigate, el } = statementSetup({});
    await fixture.whenStable();

    const select = el.querySelector<HTMLSelectElement>('select')!;
    select.value = 'amount-asc';
    select.dispatchEvent(new Event('change'));
    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { sort: 'amount', order: 'asc' } }));

    [...el.querySelectorAll<HTMLButtonElement>('nav[aria-label="Pages"] button')].find((b) => b.textContent === 'Next')!.click();
    expect(navigate).toHaveBeenLastCalledWith([], expect.objectContaining({ queryParams: { page: '1' } }));
  });

  it('applies a custom single day', async () => {
    const { fixture, navigate, el } = statementSetup({});
    await fixture.whenStable();

    [...el.querySelectorAll<HTMLButtonElement>('[aria-label="Period"] button')].find((b) => b.textContent?.includes('Custom'))!.click();
    await fixture.whenStable();
    const [from, to] = [...el.querySelectorAll<HTMLInputElement>('form input[type="date"]')];
    from.value = '2026-03-11';
    from.dispatchEvent(new Event('input'));
    to.value = '2026-03-11';
    to.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    el.querySelector<HTMLButtonElement>('form button[type="submit"]')!.click();

    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { period: 'custom', from: '2026-03-11', to: '2026-03-11' } }));
  });

  it('does not apply a reversed custom range and says why', async () => {
    const { fixture, navigate, el } = statementSetup({ period: 'custom', from: '2026-03-10', to: '2026-03-12' });
    await fixture.whenStable();
    const [from] = [...el.querySelectorAll<HTMLInputElement>('form input[type="date"]')];
    from.value = '2026-03-20';
    from.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(el.querySelector('form [role="alert"]')?.textContent).toContain('must not be after');
    expect(el.querySelector<HTMLButtonElement>('form button[type="submit"]')!.disabled).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('shows the empty state with a way out', async () => {
    const { fixture, el } = statementSetup({}, () => of({ ...PAGE, transaction: [], totalElements: 0 }));
    await fixture.whenStable();
    expect(el.textContent).toContain('No transactions in this period');
    expect(el.textContent).toContain('Show the last 90 days');
  });

  it('shows the error with its request id and retries', async () => {
    const error: ApiError = { status: 500, code: 'INTERNAL_SERVER_ERROR', message: 'Unexpected error.', requestId: 'req-7' };
    const { fixture, api, el } = statementSetup({}, () => throwError(() => error));
    await fixture.whenStable();
    expect(el.textContent).toContain('Request id: req-7');

    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Try again'))!.click();
    expect(api.list).toHaveBeenCalledTimes(2);
  });

  it('links every row to its detail by id', async () => {
    const { fixture, el } = statementSetup({});
    await fixture.whenStable();
    expect(el.querySelector('a[href="/transactions/6c6fa8c9-743a-4c79-9cf6-e1d5e25ad4a5"]')).not.toBeNull();
  });
});

function detailSetup(get: () => unknown) {
  const api = { get: vi.fn(get) };
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: StatementApi, useValue: api }, { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: ROW.id })) } }],
  });
  const fixture = TestBed.createComponent(DetailPage);
  return { fixture, api, el: fixture.nativeElement as HTMLElement };
}

describe('DetailPage', () => {
  it('shows the label, the signed amount, the status and a shortened id', async () => {
    const { fixture, el } = detailSetup(() => of(ROW));
    await fixture.whenStable();

    expect(el.querySelector('h1')?.textContent).toContain('Pix sent');
    expect(el.textContent).toContain('− R$ 12.50');
    expect(el.textContent).toContain('Registered');
    expect(el.textContent).toContain('6c6fa8c9…d4a5');
  });

  it('copies the full id and confirms with a toast', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    const { fixture, el } = detailSetup(() => of(ROW));
    await fixture.whenStable();

    el.querySelector<HTMLButtonElement>('button[aria-label="Copy transaction id"]')!.click();
    await fixture.whenStable();

    expect(writeText).toHaveBeenCalledWith(ROW.id);
  });

  it('shows "Transaction not found" for an unknown or foreign id (404)', async () => {
    const error: ApiError = { status: 404, code: 'NOT_FOUND', message: 'Transaction not found' };
    const { fixture, el } = detailSetup(() => throwError(() => error));
    await fixture.whenStable();
    expect(el.textContent).toContain('Transaction not found');
    expect(el.querySelector('app-empty-state')).not.toBeNull();
  });

  it('shows an error with a retry for any other failure', async () => {
    const error: ApiError = { status: 500, code: 'X', message: 'Unexpected error.', requestId: 'r1' };
    const { fixture, api, el } = detailSetup(() => throwError(() => error));
    await fixture.whenStable();
    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Try again'))!.click();
    expect(api.get).toHaveBeenCalledTimes(2);
  });
});

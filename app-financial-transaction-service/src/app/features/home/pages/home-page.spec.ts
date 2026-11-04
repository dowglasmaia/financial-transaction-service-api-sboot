import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { SessionService } from '../../../core/auth/session.service';
import { FakeSession } from '../../../core/auth/testing/fake-session';
import { ApiError } from '../../../core/http/api-error';
import { AccountApi, AccountMe } from '../../account/data-access/account.api';
import { StatementApi } from '../../statement/data-access/statement.api';
import { HomePage } from './home-page';

const ACCOUNT: AccountMe = { agency: '0211', number: '2022', balance: 5000 };

const SUMMARY = { sumIncoming: 100, sumOutgoing: 35, balance: 5000, series: [{ date: '2026-03-10', incoming: 100, outgoing: 0 }, { date: '2026-03-11', incoming: 0, outgoing: 35 }] };
const ROWS = { transaction: [{ id: 'a1', transactionType: 'DEPOSIT', amount: 100, dateHour: '2026-03-10T12:00:00Z' }], totalElements: 1 };

function setup(me: () => unknown, ledger: { summary?: () => unknown; list?: () => unknown } = {}) {
  const session = new FakeSession();
  session.state.set('authenticated');
  session.profile.set({ name: 'Maria Silva', email: 'maria@example.test', picture: '' });
  const api = { me: vi.fn(me) };
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      { provide: SessionService, useValue: session },
      { provide: AccountApi, useValue: api },
      { provide: StatementApi, useValue: { summary: vi.fn(ledger.summary ?? (() => of(SUMMARY))), list: vi.fn(ledger.list ?? (() => of(ROWS))) } },
    ],
  });
  const navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  const fixture = TestBed.createComponent(HomePage);
  return { fixture, api, navigateByUrl, el: fixture.nativeElement as HTMLElement };
}

describe('HomePage', () => {
  it('shows a loading placeholder while the account is being fetched', async () => {
    const pending = new Subject<AccountMe>();
    const { fixture, el } = setup(() => pending);
    await fixture.whenStable();

    expect(el.querySelector('[aria-busy="true"]')).not.toBeNull();
    expect(el.querySelector('app-balance-card')).toBeNull();
  });

  it('greets the customer by first name and shows agency, account and the backend balance', async () => {
    const { fixture, el } = setup(() => of(ACCOUNT));
    await fixture.whenStable();

    expect(el.querySelector('h1')?.textContent).toContain('Hello, Maria!');
    expect(el.textContent).toContain('Agency 0211 · Account 2022');
    expect(el.querySelector('.sr-only')?.textContent).toContain('R$ 5,000.00');
  });

  it('shows the error, the request id and a retry that asks the backend again', async () => {
    const error: ApiError = { status: 500, code: 'INTERNAL_SERVER_ERROR', message: 'Unexpected error.', requestId: 'req-42' };
    const { fixture, api, el } = setup(() => throwError(() => error));
    await fixture.whenStable();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Unexpected error.');
    expect(el.textContent).toContain('Request id: req-42');

    el.querySelector<HTMLButtonElement>('button')!.click();
    await fixture.whenStable();
    expect(api.me).toHaveBeenCalledTimes(2);
  });

  it('a valid sign-in that is not linked to a customer (403) goes to the "no access" page', async () => {
    const error: ApiError = { status: 403, code: 'FORBIDDEN', message: 'Access denied' };
    const { fixture, navigateByUrl } = setup(() => throwError(() => error));
    await fixture.whenStable();

    expect(navigateByUrl).toHaveBeenCalledWith('/no-access', { replaceUrl: true });
  });

  it('shows the backend totals, the chart with its table fallback and the recent rows', async () => {
    const { fixture, el } = setup(() => of(ACCOUNT));
    await fixture.whenStable();

    expect(el.querySelector('[aria-label="Totals of the period"]')?.textContent).toContain('R$ 100.00');
    expect(el.querySelector('app-line-chart svg')).not.toBeNull();
    expect(el.querySelector('[aria-label="Recent transactions"] app-ledger-row')).not.toBeNull();
    expect(el.textContent).toContain('See all');

    [...el.querySelectorAll<HTMLButtonElement>('app-line-chart button')].find((b) => b.textContent?.includes('View as table'))!.click();
    await fixture.whenStable();
    expect(el.querySelector('app-line-chart table')?.textContent).toContain('R$ 35.00');
  });

  it('reloads the summary when another period is chosen', async () => {
    const { fixture, el } = setup(() => of(ACCOUNT));
    await fixture.whenStable();
    [...el.querySelectorAll<HTMLButtonElement>('[aria-label="Period"] button')].find((b) => b.textContent?.includes('7 days'))!.click();
    await fixture.whenStable();
    expect(el.querySelector('[aria-label="Period"] button[aria-pressed="true"]')?.textContent).toContain('7 days');
  });

  it('shows the empty state when there is no activity', async () => {
    const { fixture, el } = setup(() => of(ACCOUNT), {
      summary: () => of({ sumIncoming: 0, sumOutgoing: 0, balance: 5000, series: [] }),
      list: () => of({ transaction: [], totalElements: 0 }),
    });
    await fixture.whenStable();
    expect(el.textContent).toContain('No activity yet. Make a deposit to get started.');
    expect(el.querySelector('app-line-chart')).toBeNull();
  });

  it('shows its own error with a retry when only the activity fails', async () => {
    const error: ApiError = { status: 500, code: 'X', message: 'Unexpected error.', requestId: 'req-9' };
    const { fixture, el } = setup(() => of(ACCOUNT), { summary: () => throwError(() => error) });
    await fixture.whenStable();
    expect(el.textContent).toContain("We couldn't load your activity");
    expect(el.textContent).toContain('Request id: req-9');
    expect(el.querySelector('app-balance-card')).not.toBeNull();
  });
});

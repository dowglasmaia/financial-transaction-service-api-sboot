import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { SessionService } from '../../../core/auth/session.service';
import { FakeSession } from '../../../core/auth/testing/fake-session';
import { ApiError } from '../../../core/http/api-error';
import { AccountApi, AccountMe } from '../../account/data-access/account.api';
import { HomePage } from './home-page';

const ACCOUNT: AccountMe = { agency: '0211', number: '2022', balance: 5000 };

function setup(me: () => unknown) {
  const session = new FakeSession();
  session.state.set('authenticated');
  session.profile.set({ name: 'Maria Silva', email: 'maria@example.test', picture: '' });
  const api = { me: vi.fn(me) };
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: SessionService, useValue: session }, { provide: AccountApi, useValue: api }],
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
});

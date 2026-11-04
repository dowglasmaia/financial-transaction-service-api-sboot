import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { SessionService } from '../../../core/auth/session.service';
import { ApiError } from '../../../core/http/api-error';
import { ToastService } from '../../../shared/ui/toast.service';
import { AccountApi, AccountMe } from '../../account/data-access/account.api';
import { DepositApi } from '../data-access/deposit.api';
import { DepositPage } from './deposit-page';

const ACCOUNT: AccountMe = { agency: '0211', number: '2022', balance: 5000 };

function setup(deposit: (...args: unknown[]) => unknown = () => of(undefined)) {
  const account = { me: vi.fn(() => of(ACCOUNT)) };
  const api = { deposit: vi.fn(deposit) };
  const session = {
    user: signal({ name: 'Maria Silva', email: 'maria@example.test', picture: '' }),
  };
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      { provide: AccountApi, useValue: account },
      { provide: DepositApi, useValue: api },
      { provide: SessionService, useValue: session },
    ],
  });
  const fixture = TestBed.createComponent(DepositPage);
  const el = fixture.nativeElement as HTMLElement;
  const input = () => el.querySelector<HTMLInputElement>('input')!;
  const button = (text: string) =>
    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.includes(text),
    );
  const type = async (value: string) => {
    input().value = value;
    input().dispatchEvent(new Event('input'));
    await fixture.whenStable();
  };
  const generate = async (amount: string) => {
    await type(amount);
    button('Generate boleto')!.click();
    await fixture.whenStable();
  };
  return { fixture, account, api, el, input, button, type, generate };
}

describe('DepositPage', () => {
  it('offers the proposed amounts and never a balance limit', async () => {
    const { fixture, el } = setup();
    await fixture.whenStable();
    for (const label of ['R$ 100', 'R$ 250', 'R$ 500', 'R$ 1000'])
      expect(el.textContent).toContain(label);
    expect(el.textContent).not.toContain('Use full balance');
    expect(el.textContent).not.toContain('exceed');
  });

  it('a preset fills the amount; zero says "Enter an amount greater than zero." and cannot generate', async () => {
    const { fixture, el, button, input, type } = setup();
    await fixture.whenStable();
    expect(button('Generate boleto')!.disabled).toBe(true);

    button('R$ 250')!.click();
    await fixture.whenStable();
    expect(input().value).toBe('250.00');
    expect(button('Generate boleto')!.disabled).toBe(false);

    await type('0');
    expect(el.textContent).toContain('Enter an amount greater than zero.');
    expect(button('Generate boleto')!.disabled).toBe(true);
  });

  it('before generating there is only a preview; generating shows the demo boleto with the payer and the amount', async () => {
    const { fixture, el, generate } = setup();
    await fixture.whenStable();
    expect(el.textContent).toContain('Preview');
    expect(el.textContent).not.toContain('Simulate payment');

    await generate('150');
    expect(el.textContent).toContain('Maia Bank (demo)');
    expect(el.textContent).toContain('Maria Silva');
    expect(el.textContent).toContain('R$ 150.00');
    expect(el.textContent).toContain('Demo. This is not a real boleto.');
    expect(el.textContent).toContain('Simulate payment');
  });

  it('download, share and copy only show "Demo action" and send nothing', async () => {
    const { fixture, api, button, generate } = setup();
    await fixture.whenStable();
    await generate('150');

    for (const label of ['Download boleto', 'Share', 'Copy line']) {
      TestBed.inject(ToastService).dismiss();
      button(label)!.click();
      await fixture.whenStable();
      expect(TestBed.inject(ToastService).message()).toBe('Demo action');
    }
    expect(api.deposit).not.toHaveBeenCalled();
  });

  it('"New amount" goes back to the form', async () => {
    const { fixture, el, button, generate } = setup();
    await fixture.whenStable();
    await generate('150');
    button('New amount')!.click();
    await fixture.whenStable();
    expect(el.textContent).not.toContain('Simulate payment');
    expect(button('Generate boleto')).toBeDefined();
  });

  it('"Simulate payment" sends ONE request with the caller\'s agency, account and the amount; a double click is one request', async () => {
    const pending = new Subject<void>();
    const { fixture, api, button, generate } = setup(() => pending);
    await fixture.whenStable();
    await generate('150');

    const pay = button('Simulate payment')!;
    pay.click();
    pay.click();
    await fixture.whenStable();

    expect(api.deposit).toHaveBeenCalledTimes(1);
    expect(api.deposit).toHaveBeenCalledWith(
      { agency: '0211', account: '2022' },
      '150.00',
      expect.any(String),
    );
  });

  it('success: "Deposit registered" and the balance is read again from the API', async () => {
    const { fixture, el, account, button, generate } = setup();
    await fixture.whenStable();
    await generate('150');
    button('Simulate payment')!.click();
    await fixture.whenStable();

    expect(el.querySelector('h1')?.textContent).toContain('Deposit registered');
    expect(el.querySelector('a[href="/transactions"]')).not.toBeNull();
    expect(account.me).toHaveBeenCalledTimes(2);
  });

  it('404 "Account not found." becomes a field error on the form and nothing is retried', async () => {
    const failure: ApiError = { status: 404, code: 'NOT_FOUND', message: 'Account not found.' };
    const { fixture, el, api, input, button, generate } = setup(() => throwError(() => failure));
    await fixture.whenStable();
    await generate('150');
    button('Simulate payment')!.click();
    await fixture.whenStable();

    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Account not found.');
    expect(input().value).toBe('150');
    expect(api.deposit).toHaveBeenCalledTimes(1);
  });

  it('422 shows the backend message in the business-failure panel', async () => {
    const failure: ApiError = {
      status: 422,
      code: 'UNPROCESSABLE_ENTITY',
      message: 'Amount must be positive and non-null',
    };
    const { fixture, el, button, generate } = setup(() => throwError(() => failure));
    await fixture.whenStable();
    await generate('150');
    button('Simulate payment')!.click();
    await fixture.whenStable();

    expect(el.querySelector('h1')?.textContent).toContain("Couldn't complete the deposit");
    expect(el.textContent).toContain('Amount must be positive and non-null');
  });

  it('uncertain: "Try again" repeats the SAME key and the same request; no automatic retry', async () => {
    let calls = 0;
    const { fixture, el, api, button, generate } = setup(() =>
      ++calls === 1
        ? throwError(() => ({ status: 0, code: 'NETWORK', message: 'offline' }))
        : of(undefined),
    );
    await fixture.whenStable();
    await generate('150');
    button('Simulate payment')!.click();
    await fixture.whenStable();

    expect(api.deposit).toHaveBeenCalledTimes(1);
    expect(el.textContent).toContain("We couldn't confirm the operation");

    button('Try again')!.click();
    await fixture.whenStable();
    expect(api.deposit).toHaveBeenCalledTimes(2);
    expect(api.deposit.mock.calls[1]).toEqual(api.deposit.mock.calls[0]);
    expect(el.querySelector('h1')?.textContent).toContain('Deposit registered');
  });
});

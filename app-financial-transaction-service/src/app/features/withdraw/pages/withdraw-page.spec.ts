import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { AccountApi, AccountMe } from '../../account/data-access/account.api';
import { WithdrawApi } from '../data-access/withdraw.api';
import { WithdrawPage } from './withdraw-page';

const ACCOUNT: AccountMe = { agency: '0211', number: '2022', balance: 5000 };
const INSUFFICIENT = 'Operation not performed, there is not enough balance in the account.';

function setup(withdraw: (amount: string, key: string) => unknown = () => of(undefined)) {
  const account = { me: vi.fn(() => of(ACCOUNT)) };
  const api = { withdraw: vi.fn(withdraw) };
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AccountApi, useValue: account }, { provide: WithdrawApi, useValue: api }],
  });
  const fixture = TestBed.createComponent(WithdrawPage);
  const el = fixture.nativeElement as HTMLElement;
  const input = () => el.querySelector<HTMLInputElement>('input')!;
  const button = (text: string) => [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes(text));
  const dialogButton = (text: string) => [...el.querySelectorAll<HTMLButtonElement>('[role="dialog"] button')].find((b) => b.textContent?.includes(text));
  const type = async (value: string) => {
    input().value = value;
    input().dispatchEvent(new Event('input'));
    await fixture.whenStable();
  };
  const review = async (amount: string) => {
    await type(amount);
    button('Review withdrawal')!.click();
    await fixture.whenStable();
  };
  return { fixture, account, api, el, input, button, dialogButton, type, review };
}

describe('WithdrawPage', () => {
  it('shows the balance from the API and the proposed amounts', async () => {
    const { fixture, el, account } = setup();
    await fixture.whenStable();

    expect(account.me).toHaveBeenCalledTimes(1);
    expect(el.textContent).toContain('R$ 5,000.00');
    for (const label of ['R$ 50', 'R$ 100', 'R$ 200', 'R$ 500', 'Use full balance']) expect(el.textContent).toContain(label);
  });

  it('a preset fills the amount and "Use full balance" fills the whole balance', async () => {
    const { fixture, button, input } = setup();
    await fixture.whenStable();

    button('R$ 200')!.click();
    await fixture.whenStable();
    expect(input().value).toBe('200.00');

    button('Use full balance')!.click();
    await fixture.whenStable();
    expect(input().value).toBe('5000.00');
  });

  it('gives feedback above the balance (the server still decides, so Review stays available) and for zero (nothing to review)', async () => {
    const { fixture, el, button, type } = setup();
    await fixture.whenStable();
    expect(button('Review withdrawal')!.disabled).toBe(true);

    await type('5000.01');
    expect(el.textContent).toContain("The amount can't exceed your available balance.");
    expect(button('Review withdrawal')!.disabled).toBe(false); // feedback only: the server answers 422 and the panel says so

    await type('0');
    expect(el.textContent).toContain('Enter an amount greater than zero.');
    expect(button('Review withdrawal')!.disabled).toBe(true);

    await type('5000');
    expect(el.textContent).not.toContain("can't exceed");
    expect(button('Review withdrawal')!.disabled).toBe(false);
  });

  it('opens a review with the amount and sends nothing until Confirm', async () => {
    const { fixture, api, el, review } = setup();
    await fixture.whenStable();
    await review('100');

    expect(el.querySelector('[role="dialog"]')?.textContent).toContain('R$ 100.00');
    expect(api.withdraw).not.toHaveBeenCalled();
  });

  it('a double click on Confirm sends exactly one request', async () => {
    const pending = new Subject<void>();
    const { fixture, api, review, dialogButton } = setup(() => pending);
    await fixture.whenStable();
    await review('100');

    const confirm = dialogButton('Confirm')!;
    confirm.click();
    confirm.click();
    await fixture.whenStable();

    expect(api.withdraw).toHaveBeenCalledTimes(1);
    expect(api.withdraw).toHaveBeenCalledWith('100.00', expect.any(String));
  });

  it('success: "Withdrawal registered", never claims cash was dispensed, and the balance is read again', async () => {
    const { fixture, el, account, review, dialogButton } = setup();
    await fixture.whenStable();
    await review('100');
    dialogButton('Confirm')!.click();
    await fixture.whenStable();

    expect(el.querySelector('h1')?.textContent).toContain('Withdrawal registered');
    expect(el.textContent).not.toMatch(/dispens/i);
    expect(el.querySelector('a[href="/transactions"]')).not.toBeNull();
    expect(el.querySelector('a[href="/home"]')).not.toBeNull();
    expect(account.me).toHaveBeenCalledTimes(2);
  });

  it('business failure: the backend message is shown, the balance is not touched, nothing is retried', async () => {
    const failure: ApiError = { status: 422, code: 'UNPROCESSABLE_ENTITY', message: INSUFFICIENT };
    const { fixture, el, api, account, review, dialogButton } = setup(() => throwError(() => failure));
    await fixture.whenStable();
    await review('100');
    dialogButton('Confirm')!.click();
    await fixture.whenStable();

    expect(el.querySelector('h1')?.textContent).toContain("Couldn't complete the withdrawal");
    expect(el.textContent).toContain(INSUFFICIENT);
    expect(api.withdraw).toHaveBeenCalledTimes(1);
    expect(account.me).toHaveBeenCalledTimes(1);
  });

  it('uncertain: says so, links to Transactions, and "Try again" repeats the SAME key', async () => {
    let calls = 0;
    const { fixture, el, api, button, review, dialogButton } = setup(() => (++calls === 1 ? throwError(() => ({ status: 0, code: 'NETWORK', message: 'offline' })) : of(undefined)));
    await fixture.whenStable();
    await review('100');
    dialogButton('Confirm')!.click();
    await fixture.whenStable();

    expect(el.textContent).toContain("We couldn't confirm the operation. Check your transactions before trying again, so you don't pay twice.");
    expect(el.querySelector('a[href="/transactions"]')).not.toBeNull();
    expect(api.withdraw).toHaveBeenCalledTimes(1);

    button('Try again')!.click();
    await fixture.whenStable();

    expect(api.withdraw).toHaveBeenCalledTimes(2);
    const [first, second] = api.withdraw.mock.calls;
    expect(second).toEqual(first); // same amount, same Idempotency-Key
    expect(el.querySelector('h1')?.textContent).toContain('Withdrawal registered');
  });

  it('a new withdrawal after a success starts a form with a new attempt key', async () => {
    const { fixture, el, api, button, review, dialogButton } = setup();
    await fixture.whenStable();
    await review('100');
    dialogButton('Confirm')!.click();
    await fixture.whenStable();

    button('New withdrawal')!.click();
    await fixture.whenStable();
    expect(el.querySelector('input')).not.toBeNull();

    await review('100');
    dialogButton('Confirm')!.click();
    await fixture.whenStable();
    expect(api.withdraw.mock.calls[1][1]).not.toBe(api.withdraw.mock.calls[0][1]);
  });
});

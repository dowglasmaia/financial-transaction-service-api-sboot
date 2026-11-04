import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { AccountApi, AccountMe } from '../../account/data-access/account.api';
import { PixApi } from '../data-access/pix.api';
import { PixPage } from './pix-page';

const ACCOUNT: AccountMe = { agency: '0211', number: '2022', balance: 5000 };
const CPF = '529.982.247-25';
const UNCERTAIN =
  "We couldn't confirm the operation. Check your transactions before trying again, so you don't pay twice.";

function setup(send: (...args: unknown[]) => unknown = () => of(undefined)) {
  const account = { me: vi.fn(() => of(ACCOUNT)) };
  const api = { send: vi.fn(send) };
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      { provide: AccountApi, useValue: account },
      { provide: PixApi, useValue: api },
    ],
  });
  const fixture = TestBed.createComponent(PixPage);
  const el = fixture.nativeElement as HTMLElement;
  const keyInput = () => el.querySelector<HTMLInputElement>('input')!;
  const button = (text: string) =>
    [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) =>
      b.textContent?.trim().includes(text),
    );
  const radio = (text: string) =>
    [...el.querySelectorAll<HTMLButtonElement>('[role="radio"]')].find((b) =>
      b.textContent?.includes(text),
    )!;
  const typeInto = async (input: HTMLInputElement, value: string) => {
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  };
  const click = async (text: string) => {
    button(text)!.click();
    await fixture.whenStable();
  };
  const toAmount = async (key = CPF) => {
    await typeInto(keyInput(), key);
    await click('Continue');
  };
  const toReview = async (amount = '50', key = CPF) => {
    await toAmount(key);
    await typeInto(keyInput(), amount);
    await click('Continue');
  };
  return {
    fixture,
    account,
    api,
    el,
    keyInput,
    button,
    radio,
    typeInto,
    click,
    toAmount,
    toReview,
  };
}

describe('PixPage', () => {
  it('starts at step 1 of 3 (Key, Amount, Review) with the hint and nothing to continue with', async () => {
    const { fixture, el, button } = setup();
    await fixture.whenStable();

    expect(el.querySelector('app-stepper')?.textContent).toContain('Step 1 of 3');
    for (const step of ['Key', 'Amount', 'Review'])
      expect(el.querySelector('app-stepper')?.textContent).toContain(step);
    expect(el.textContent).toContain('Check the key with the person you are paying.');
    expect(button('Continue')!.disabled).toBe(true);
  });

  it('a CPF is masked while typing; a wrong one explains itself; a valid one lets you continue', async () => {
    const { fixture, el, keyInput, button, typeInto } = setup();
    await fixture.whenStable();

    await typeInto(keyInput(), '11111111111');
    expect(keyInput().value).toBe('111.111.111-11');
    expect(el.textContent).toContain('Enter a valid CPF, for example 529.982.247-25.');
    expect(button('Continue')!.disabled).toBe(true);

    await typeInto(keyInput(), '52998224725');
    expect(keyInput().value).toBe(CPF);
    expect(el.textContent).not.toContain('Enter a valid CPF');
    expect(button('Continue')!.disabled).toBe(false);
  });

  it("changing the key type clears the key and applies that type's rule (e-mail, mobile)", async () => {
    const { fixture, el, keyInput, button, radio, typeInto } = setup();
    await fixture.whenStable();
    await typeInto(keyInput(), CPF);

    radio('E-mail').click();
    await fixture.whenStable();
    expect(keyInput().value).toBe('');
    await typeInto(keyInput(), 'a..b@example.com');
    expect(el.textContent).toContain('Enter a valid e-mail address.');
    await typeInto(keyInput(), 'maria@example.com');
    expect(button('Continue')!.disabled).toBe(false);

    radio('Mobile').click();
    await fixture.whenStable();
    await typeInto(keyInput(), '11912345678');
    expect(keyInput().value).toBe('(11) 9 1234-5678');
    expect(button('Continue')!.disabled).toBe(false);
  });

  it('step 2 offers 50/100/200/500 and the full balance, and blocks an amount above the balance', async () => {
    const { fixture, el, keyInput, button, typeInto, toAmount } = setup();
    await fixture.whenStable();
    await toAmount();

    expect(el.querySelector('app-stepper')?.textContent).toContain('Step 2 of 3');
    for (const label of ['R$ 50', 'R$ 100', 'R$ 200', 'R$ 500', 'Use full balance'])
      expect(el.textContent).toContain(label);
    expect(button('Continue')!.disabled).toBe(true);

    await typeInto(keyInput(), '5000.01');
    expect(el.textContent).toContain("The amount can't exceed your available balance.");
    expect(button('Continue')!.disabled).toBe(true);

    await typeInto(keyInput(), '5000');
    expect(button('Continue')!.disabled).toBe(false);
  });

  it('Back keeps what was typed', async () => {
    const { fixture, keyInput, click, toReview } = setup();
    await fixture.whenStable();
    await toReview('50');

    await click('Back'); // review -> amount
    expect(keyInput().value).toBe('50');
    await click('Back'); // amount -> key
    expect(keyInput().value).toBe(CPF);
  });

  it('the review shows type, MASKED key, amount and the account; the full key is nowhere on the page', async () => {
    const { fixture, el, toReview } = setup();
    await fixture.whenStable();
    await toReview('50');

    expect(el.querySelector('app-stepper')?.textContent).toContain('Step 3 of 3');
    for (const text of ['CPF', '•••.982.•••-25', 'R$ 50.00', 'Your account'])
      expect(el.textContent).toContain(text);
    expect(el.textContent).not.toContain('52998224725');
    expect(el.textContent).not.toContain(CPF);
  });

  it('Confirm and send: a double click sends ONE request with the plain key', async () => {
    const pending = new Subject<void>();
    const { fixture, api, button, toReview } = setup(() => pending);
    await fixture.whenStable();
    await toReview('50');

    const confirm = button('Confirm and send')!;
    confirm.click();
    confirm.click();
    await fixture.whenStable();

    expect(api.send).toHaveBeenCalledTimes(1);
    expect(api.send).toHaveBeenCalledWith('cpf', '52998224725', '50.00', expect.any(String));
  });

  it('success: "Pix sent" with a masked receipt, the balance is read again', async () => {
    const { fixture, el, account, click, toReview } = setup();
    await fixture.whenStable();
    await toReview('50');
    await click('Confirm and send');

    expect(el.querySelector('h1')?.textContent).toContain('Pix sent');
    expect(el.textContent).toContain('•••.982.•••-25');
    expect(el.textContent).toContain('R$ 50.00');
    expect(el.textContent).not.toContain('52998224725');
    expect(el.querySelector('a[href="/transactions"]')).not.toBeNull();
    expect(account.me).toHaveBeenCalledTimes(2);
  });

  it("business failure: the balance was not changed, the reason is the backend's, nothing is retried", async () => {
    const failure: ApiError = {
      status: 422,
      code: 'UNPROCESSABLE_ENTITY',
      message: 'Fail send Pix.',
    };
    const { fixture, el, api, account, click, toReview } = setup(() => throwError(() => failure));
    await fixture.whenStable();
    await toReview('50');
    await click('Confirm and send');

    expect(el.textContent).toContain("We couldn't send this Pix. Your balance was not changed.");
    expect(el.textContent).toContain('Fail send Pix.');
    expect(api.send).toHaveBeenCalledTimes(1);
    expect(account.me).toHaveBeenCalledTimes(1);
  });

  it('uncertain: says so, links to Transactions, and "Try again" repeats the SAME key', async () => {
    let calls = 0;
    const { fixture, el, api, click, toReview } = setup(() =>
      ++calls === 1
        ? throwError(() => ({ status: 0, code: 'NETWORK', message: 'offline' }))
        : of(undefined),
    );
    await fixture.whenStable();
    await toReview('50');
    await click('Confirm and send');

    expect(el.textContent).toContain(UNCERTAIN);
    expect(el.querySelector('a[href="/transactions"]')).not.toBeNull();
    expect(api.send).toHaveBeenCalledTimes(1);

    await click('Try again');
    expect(api.send).toHaveBeenCalledTimes(2);
    expect(api.send.mock.calls[1]).toEqual(api.send.mock.calls[0]);
    expect(el.querySelector('h1')?.textContent).toContain('Pix sent');
  });

  it('after a business failure "Edit and try again" returns to the review; the key never reaches the console', async () => {
    const spies = (['log', 'info', 'warn', 'error', 'debug'] as const).map((method) =>
      vi.spyOn(console, method).mockImplementation(() => undefined),
    );
    const failure: ApiError = {
      status: 422,
      code: 'UNPROCESSABLE_ENTITY',
      message: 'Fail send Pix.',
    };
    const { fixture, el, click, toReview } = setup(() => throwError(() => failure));
    await fixture.whenStable();
    await toReview('50');
    await click('Confirm and send');
    await click('Edit and try again');

    expect(el.querySelector('app-stepper')?.textContent).toContain('Step 3 of 3');
    for (const spy of spies) expect(JSON.stringify(spy.mock.calls)).not.toContain('52998224725');
    spies.forEach((spy) => spy.mockRestore());
  });

  it('a new Pix after a success starts empty with a new attempt key', async () => {
    const { fixture, el, api, keyInput, click, toReview } = setup();
    await fixture.whenStable();
    await toReview('50');
    await click('Confirm and send');
    await click('New Pix');

    expect(el.querySelector('app-stepper')?.textContent).toContain('Step 1 of 3');
    expect(keyInput().value).toBe('');
    await toReview('20');
    await click('Confirm and send');
    expect(api.send.mock.calls[1][3]).not.toBe(api.send.mock.calls[0][3]);
  });
});

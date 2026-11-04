import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ApiError } from '../../../core/http/api-error';
import { AccountApi, AccountMe } from '../../account/data-access/account.api';
import { RechargeApi } from '../data-access/recharge.api';
import { RechargePage } from './recharge-page';

const ACCOUNT: AccountMe = { agency: '0211', number: '2022', balance: 5000 };
const SUCCESS = 'Recharge requested. Your carrier will apply the credit shortly.';

function setup(recharge: (...args: unknown[]) => unknown = () => of(undefined)) {
  const account = { me: vi.fn(() => of(ACCOUNT)) };
  const api = { recharge: vi.fn(recharge) };
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AccountApi, useValue: account }, { provide: RechargeApi, useValue: api }],
  });
  const fixture = TestBed.createComponent(RechargePage);
  const el = fixture.nativeElement as HTMLElement;
  const phone = () => el.querySelector<HTMLInputElement>('input[type="tel"]')!;
  const amount = () => el.querySelector<HTMLInputElement>('input[inputmode="decimal"]')!;
  const button = (text: string) => [...el.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes(text));
  const dialogButton = (text: string) => [...el.querySelectorAll<HTMLButtonElement>('[role="dialog"] button')].find((b) => b.textContent?.includes(text));
  const typeInto = async (input: HTMLInputElement, value: string) => {
    input.value = value;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  };
  const fill = async (carrier = 'Vivo', digits = '11912345678', value = '30') => {
    button(carrier)!.click();
    await typeInto(phone(), digits);
    await typeInto(amount(), value);
  };
  const review = async (...args: [string?, string?, string?]) => {
    await fill(...args);
    button('Review recharge')!.click();
    await fixture.whenStable();
  };
  return { fixture, account, api, el, phone, amount, button, dialogButton, typeInto, fill, review };
}

describe('RechargePage', () => {
  it('offers the four carriers of the backend (Algar, not Oi) and the proposed amounts 15/30/50/100', async () => {
    const { fixture, el, button } = setup();
    await fixture.whenStable();

    for (const name of ['Claro', 'Vivo', 'TIM', 'Algar']) expect(button(name)).toBeDefined();
    expect(el.textContent).not.toContain('Oi');
    for (const label of ['R$ 15', 'R$ 30', 'R$ 50', 'R$ 100']) expect(el.textContent).toContain(label);
  });

  it('selecting a carrier marks only that one as pressed', async () => {
    const { fixture, button } = setup();
    await fixture.whenStable();

    button('TIM')!.click();
    await fixture.whenStable();
    expect(button('TIM')!.getAttribute('aria-pressed')).toBe('true');
    expect(button('Vivo')!.getAttribute('aria-pressed')).toBe('false');

    button('Claro')!.click();
    await fixture.whenStable();
    expect(button('TIM')!.getAttribute('aria-pressed')).toBe('false');
    expect(button('Claro')!.getAttribute('aria-pressed')).toBe('true');
  });

  it('masks the phone as (11) 9 1234-5678 while typing', async () => {
    const { fixture, phone, typeInto } = setup();
    await fixture.whenStable();
    await typeInto(phone(), '11912345678');
    expect(phone().value).toBe('(11) 9 1234-5678');
    await typeInto(phone(), '(11) 9 1234-56789999');
    expect(phone().value).toBe('(11) 9 1234-5678');
  });

  it('Review needs a carrier, a valid mobile number and an amount; a bad number explains itself', async () => {
    const { fixture, el, button, phone, amount, typeInto } = setup();
    await fixture.whenStable();
    expect(button('Review recharge')!.disabled).toBe(true);

    button('Vivo')!.click();
    await typeInto(amount(), '30');
    expect(button('Review recharge')!.disabled).toBe(true); // no number yet

    await typeInto(phone(), '11812345678'); // 8 where the mobile 9 must be
    expect(el.textContent).toContain('Enter a valid mobile number, for example (11) 9 1234-5678.');
    expect(button('Review recharge')!.disabled).toBe(true);

    await typeInto(phone(), '11912345678');
    expect(el.textContent).not.toContain('Enter a valid mobile number');
    expect(button('Review recharge')!.disabled).toBe(false);
  });

  it('the review shows carrier, number and amount and sends nothing until Confirm', async () => {
    const { fixture, api, el, review } = setup();
    await fixture.whenStable();
    await review('Vivo', '11912345678', '30');

    const dialog = el.querySelector('[role="dialog"]')!.textContent!;
    expect(dialog).toContain('Vivo');
    expect(dialog).toContain('(11) 9 1234-5678');
    expect(dialog).toContain('R$ 30.00');
    expect(api.recharge).not.toHaveBeenCalled();
  });

  it('a double click on Confirm sends exactly one request, split into areaCode + phoneNumber', async () => {
    const pending = new Subject<void>();
    const { fixture, api, review, dialogButton } = setup(() => pending);
    await fixture.whenStable();
    await review('Vivo', '11912345678', '30');

    const confirm = dialogButton('Confirm')!;
    confirm.click();
    confirm.click();
    await fixture.whenStable();

    expect(api.recharge).toHaveBeenCalledTimes(1);
    expect(api.recharge).toHaveBeenCalledWith({ areaCode: 11, phoneNumber: 912345678, carrier: 'VIVO' }, '30.00', expect.any(String));
  });

  it('success says the recharge was "requested" and never "completed"; the balance is read again', async () => {
    const { fixture, el, account, review, dialogButton } = setup();
    await fixture.whenStable();
    await review();
    dialogButton('Confirm')!.click();
    await fixture.whenStable();

    expect(el.querySelector('h1')?.textContent).toContain('Recharge requested');
    expect(el.textContent).toContain(SUCCESS);
    expect(el.textContent).not.toMatch(/completed/i);
    expect(el.querySelector('a[href="/transactions"]')).not.toBeNull();
    expect(account.me).toHaveBeenCalledTimes(2);
  });

  it('business failure shows the backend message, never says completed, and retries nothing', async () => {
    const failure: ApiError = { status: 422, code: 'UNPROCESSABLE_ENTITY', message: 'Invalid cell phone numer' };
    const { fixture, el, api, account, review, dialogButton } = setup(() => throwError(() => failure));
    await fixture.whenStable();
    await review();
    dialogButton('Confirm')!.click();
    await fixture.whenStable();

    expect(el.querySelector('h1')?.textContent).toContain("Couldn't request the recharge");
    expect(el.textContent).toContain('Invalid cell phone numer');
    expect(el.textContent).not.toMatch(/completed/i);
    expect(api.recharge).toHaveBeenCalledTimes(1);
    expect(account.me).toHaveBeenCalledTimes(1);
  });

  it('uncertain: "Try again" repeats the SAME key and request', async () => {
    let calls = 0;
    const { fixture, el, api, button, review, dialogButton } = setup(() => (++calls === 1 ? throwError(() => ({ status: 0, code: 'NETWORK', message: 'offline' })) : of(undefined)));
    await fixture.whenStable();
    await review();
    dialogButton('Confirm')!.click();
    await fixture.whenStable();

    expect(el.textContent).toContain("We couldn't confirm the operation");
    expect(api.recharge).toHaveBeenCalledTimes(1);

    button('Try again')!.click();
    await fixture.whenStable();
    expect(api.recharge).toHaveBeenCalledTimes(2);
    expect(api.recharge.mock.calls[1]).toEqual(api.recharge.mock.calls[0]);
    expect(el.textContent).toContain(SUCCESS);
  });

  it('arrives pre-filled from the Home quick-recharge card and opens the same review', async () => {
    history.replaceState({ recharge: { carrier: 'TIM', phone: '(21) 9 8765-4321', amount: '50.00' } }, '');
    const { fixture, el, api } = setup();
    await fixture.whenStable();
    history.replaceState(null, '');

    const dialog = el.querySelector('[role="dialog"]')!.textContent!;
    expect(dialog).toContain('TIM');
    expect(dialog).toContain('(21) 9 8765-4321');
    expect(dialog).toContain('R$ 50.00');
    expect(api.recharge).not.toHaveBeenCalled();
  });
});

import { isValidPixKey, maskPixInput, maskPixKeyForDisplay, pixKeyFeedback, toWireKey } from './pix-key';

/**
 * The accept/reject cases are the SAME lists as the backend's `PixKeyRulesTest`: if one side changes a rule, the other
 * must change with it. They run against the wire value (what is sent), exactly what the server validates.
 */
const VALID_CPF = ['52998224725', '529.982.247-25', '82215344040', '822.153.440-40', '11144477735'];
const INVALID_CPF = ['', '1', '5299822472', '529982247251', '52998224724', '529.982.247-24', '00000000000', '111.111.111-11', '99999999999', '529.982.24725', '529982.247-25', 'abcdefghijk', ' 52998224725', '529 982 247 25', '5299822472a'];
const VALID_EMAIL = ['a.b-c@example.com', 'user+tag@mail.example.com.br', 'a_b@x.io', 'maria@maiabank.com.br', 'x@museum.museum'];
const INVALID_EMAIL = ['', 'nope', 'a@b', '@example.com', 'a@.com', 'a..b@example.com', '.a@example.com', 'a.@example.com', 'a@-example.com', 'a@example-.com', 'a@example..com', 'a b@example.com', 'a@example.c', 'a@example.c0m', 'a@@example.com', `${'a'.repeat(64)}@example.com`];
const VALID_PHONE = ['11999998888', '21912345678', '99900000000'];
const INVALID_PHONE = ['', '1199999888', '119999988888', '11899998888', '01999998888', '10999998888', '(11)99999-8888', '+5511999998888', '11 99999 8888', '11a99998888'];

describe('Pix key rules (identical to the backend cases)', () => {
  it.each(VALID_CPF)('cpf %j is valid', (value) => expect(isValidPixKey('cpf', value)).toBe(true));
  it.each(INVALID_CPF)('cpf %j is invalid', (value) => expect(isValidPixKey('cpf', value)).toBe(false));
  it.each(VALID_EMAIL)('email %j is valid', (value) => expect(isValidPixKey('email', value)).toBe(true));
  it.each(INVALID_EMAIL)('email %j is invalid', (value) => expect(isValidPixKey('email', value)).toBe(false));
  it.each(VALID_PHONE)('phone %j is valid', (value) => expect(isValidPixKey('phone', value)).toBe(true));
  it.each(INVALID_PHONE)('phone %j is invalid', (value) => expect(isValidPixKey('phone', value)).toBe(false));
});

describe('typing a key', () => {
  it('masks a CPF as 999.999.999-99 and a mobile as (11) 9 1234-5678; an e-mail is left as typed', () => {
    expect(maskPixInput('cpf', '52998224725')).toBe('529.982.247-25');
    expect(maskPixInput('cpf', '5299')).toBe('529.9');
    expect(maskPixInput('cpf', '529.982.247-25999')).toBe('529.982.247-25');
    expect(maskPixInput('phone', '11912345678')).toBe('(11) 9 1234-5678');
    expect(maskPixInput('email', ' Maria@Example.com')).toBe(' Maria@Example.com');
  });

  it('sends digits only for a CPF and a mobile, and the trimmed e-mail', () => {
    expect(toWireKey('cpf', '529.982.247-25')).toBe('52998224725');
    expect(toWireKey('phone', '(11) 9 1234-5678')).toBe('11912345678');
    expect(toWireKey('email', '  maria@example.com ')).toBe('maria@example.com');
  });

  it('gives the wire key when valid and a message otherwise (nothing while the field is empty)', () => {
    expect(pixKeyFeedback('cpf', '529.982.247-25')).toEqual({ key: '52998224725', message: null });
    expect(pixKeyFeedback('cpf', '')).toEqual({ key: null, message: null });
    expect(pixKeyFeedback('cpf', '111.111.111-11').message).toBe('Enter a valid CPF, for example 529.982.247-25.');
    expect(pixKeyFeedback('email', 'a..b@example.com').message).toBe('Enter a valid e-mail address.');
    expect(pixKeyFeedback('phone', '(11) 8 1234-5678').message).toBe('Enter a valid mobile number, for example (11) 9 1234-5678.');
  });

  it('shows a key only masked in the review and the receipt', () => {
    expect(maskPixKeyForDisplay('cpf', '52998224725')).toBe('•••.982.•••-25');
    expect(maskPixKeyForDisplay('phone', '11912345678')).toBe('(11) 9 ••••-5678');
    expect(maskPixKeyForDisplay('email', 'maria@example.com')).toBe('m•••@example.com');
  });
});

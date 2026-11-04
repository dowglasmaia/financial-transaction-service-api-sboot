import { maskPhone, splitPhone } from './phone';

describe('maskPhone', () => {
  it.each([
    ['', ''],
    ['1', '(1'],
    ['11', '(11'],
    ['119', '(11) 9'],
    ['1191', '(11) 9 1'],
    ['11912345', '(11) 9 1234-5'],
    ['11912345678', '(11) 9 1234-5678'],
    ['119123456789999', '(11) 9 1234-5678'], // never more than 11 digits
    ['(11) 9 1234-5678', '(11) 9 1234-5678'], // already masked
    ['ab11-9x12', '(11) 9 12'], // letters and symbols are dropped
  ])('%j → %j', (typed, masked) => {
    expect(maskPhone(typed)).toBe(masked);
  });
});

describe('splitPhone (area code + number, the backend rule ^[1-9]{2}9[0-9]{8}$)', () => {
  it('splits a masked number into areaCode and phoneNumber as integers', () => {
    expect(splitPhone('(11) 9 1234-5678')).toEqual({ areaCode: 11, phoneNumber: 912345678 });
    expect(splitPhone('(21) 9 0000-0001')).toEqual({ areaCode: 21, phoneNumber: 900000001 });
  });

  it.each(['', '(11) 9 1234-567', '(11) 8 1234-5678', '(01) 9 1234-5678', '(10) 9 1234-5678', '(11) 9 1234-56789'])('%j is not a valid mobile number', (value) => {
    expect(splitPhone(value)).toBeNull();
  });
});

import { formatBrl, parseAmountInput } from './money';

describe('formatBrl', () => {
  it('formats with the R$ prefix, thousands separator and two decimals', () => {
    expect(formatBrl(12480.5)).toBe('R$ 12,480.50');
    expect(formatBrl('5200')).toBe('R$ 5,200.00');
    expect(formatBrl(0)).toBe('R$ 0.00');
  });

  it('formats negative values with the minus before the symbol', () => {
    expect(formatBrl(-250)).toBe('-R$ 250.00');
  });
});

describe('parseAmountInput', () => {
  it('normalizes valid input to a two-decimal string', () => {
    expect(parseAmountInput('1,234.5')).toBe('1234.50');
    expect(parseAmountInput('200')).toBe('200.00');
    expect(parseAmountInput(' 0.99 ')).toBe('0.99');
  });

  it('rejects empty, zero, negative, more than two decimals and non-numbers', () => {
    for (const bad of ['', '0', '0.00', '-5', '1.234', 'abc', '1.2.3', 'R$ 5']) {
      expect(parseAmountInput(bad)).toBeNull();
    }
  });
});

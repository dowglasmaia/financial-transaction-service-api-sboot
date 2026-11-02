const BRL = new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** `R$ 12,480.50` — the account is in Brazilian reais, copy is English (en-US number format). */
export function formatBrl(value: number | string): string {
  const amount = typeof value === 'string' ? Number(value) : value;
  const sign = amount < 0 ? '-' : '';
  return `${sign}R$ ${BRL.format(Math.abs(amount))}`;
}

/**
 * Normalizes what the user typed into a two-decimal string (`"1,234.5"` → `"1234.50"`), or `null` when it is
 * not a positive amount with at most two decimals. The UI only gives early feedback; the backend decides.
 */
export function parseAmountInput(text: string): string | null {
  const cleaned = text.trim().replace(/,/g, '');
  if (!/^\d+(\.\d{1,2})?$/.test(cleaned)) return null;
  const [integer, decimals = ''] = cleaned.split('.');
  const normalized = `${integer.replace(/^0+(?=\d)/, '')}.${decimals.padEnd(2, '0')}`;
  return Number(normalized) > 0 ? normalized : null;
}

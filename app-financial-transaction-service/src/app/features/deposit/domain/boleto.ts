/** A DEMO boleto: built on the device from the amount, never issued, stored or fetched. It is not payable anywhere. */
export interface Boleto {
  readonly amount: string;
  readonly beneficiary: string;
  readonly payer: string;
  readonly document: string;
  readonly dueDate: string;
  readonly digitableLine: string;
  /** Widths (1 to 4) of the decorative bars. */
  readonly bars: readonly number[];
}

const BAR_COUNT = 56;
const DUE_IN_DAYS = 3;

/** Deterministic: the same amount always gives the same document, line and bars (`today` only moves the due date and year). */
export function buildBoleto(amount: string, payer: string, today: Date): Boleto {
  const cents = Math.round(Number(amount) * 100);
  const bars: number[] = [];
  let state = cents + 7;
  for (let i = 0; i < BAR_COUNT; i++) {
    state = Number((BigInt(state) * 1103515245n + 12345n) & 0x7fffffffn);
    bars.push(1 + (state % 4));
  }
  const due = new Date(today.getFullYear(), today.getMonth(), today.getDate() + DUE_IN_DAYS);
  return {
    amount,
    beneficiary: 'Maia Bank (demo)',
    payer,
    document: `BOL-${today.getFullYear()}-${String(4200 + (cents % 800)).padStart(5, '0')}`,
    dueDate: due.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }),
    digitableLine: `23793.38128 60000.000003 00000.000400 1 ${String(cents).padStart(14, '0')}`,
    bars,
  };
}

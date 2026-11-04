import { buildBoleto } from './boleto';

const TODAY = new Date(2026, 10, 4); // 4 Nov 2026

describe('buildBoleto (a demo: nothing here is a real boleto)', () => {
  it('is deterministic: the same amount always gives the same document, line and bars', () => {
    expect(buildBoleto('150.00', 'Maria Silva', TODAY)).toEqual(
      buildBoleto('150.00', 'Maria Silva', TODAY),
    );
  });

  it('a different amount gives a different document line and bars', () => {
    const a = buildBoleto('150.00', 'Maria Silva', TODAY);
    const b = buildBoleto('150.01', 'Maria Silva', TODAY);
    expect(a.digitableLine).not.toBe(b.digitableLine);
    expect(a.bars).not.toEqual(b.bars);
  });

  it('carries the amount, the payer, a demo beneficiary and a due date three days ahead', () => {
    const boleto = buildBoleto('150.00', 'Maria Silva', TODAY);
    expect(boleto.amount).toBe('150.00');
    expect(boleto.payer).toBe('Maria Silva');
    expect(boleto.beneficiary).toBe('Maia Bank (demo)');
    expect(boleto.dueDate).toBe('07 Nov 2026');
    expect(boleto.document).toMatch(/^BOL-2026-\d{5}$/);
  });

  it('the digitable line ends with the amount in cents and the bars are 56 widths from 1 to 4', () => {
    const boleto = buildBoleto('150.00', 'Maria Silva', TODAY);
    expect(boleto.digitableLine.endsWith('00000000015000')).toBe(true);
    expect(boleto.bars).toHaveLength(56);
    expect(boleto.bars.every((width) => width >= 1 && width <= 4)).toBe(true);
  });
});

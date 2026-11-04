import { buildChart } from './chart';

describe('buildChart', () => {
  const days = [
    { date: '2026-03-10', incoming: 100, outgoing: 0 },
    { date: '2026-03-11', incoming: 0, outgoing: 50 },
    { date: '2026-03-12', incoming: 200, outgoing: 25 },
  ];

  it('scales both lines to the same axis with the largest value at the top', () => {
    const chart = buildChart(days, 300, 100);
    expect(chart.max).toBe(200);
    expect(chart.incoming).toHaveLength(3);
    expect(chart.incoming[2]).toEqual({ x: 300, y: 0 });
    expect(chart.incoming[0].x).toBe(0);
    expect(chart.outgoing[1]).toEqual({ x: 150, y: 75 });
    expect(chart.incomingPath.startsWith('M0,')).toBe(true);
  });

  it('draws a flat line on the baseline when there is no money at all', () => {
    const chart = buildChart([{ date: '2026-03-10', incoming: 0, outgoing: 0 }, { date: '2026-03-11', incoming: 0, outgoing: 0 }], 200, 80);
    expect(chart.max).toBe(0);
    expect(chart.incoming.every((p) => p.y === 80)).toBe(true);
  });

  it('puts a single day in the middle', () => {
    expect(buildChart([{ date: '2026-03-10', incoming: 10, outgoing: 0 }], 200, 80).incoming[0].x).toBe(100);
  });

  it('is empty for no days', () => {
    expect(buildChart([], 200, 80).incomingPath).toBe('');
  });
});

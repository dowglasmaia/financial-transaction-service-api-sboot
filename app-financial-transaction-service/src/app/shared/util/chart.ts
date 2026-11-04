export interface DayPoint {
  readonly date: string;
  readonly incoming: number;
  readonly outgoing: number;
}

export interface XY {
  readonly x: number;
  readonly y: number;
}

export interface Chart {
  readonly max: number;
  readonly incoming: XY[];
  readonly outgoing: XY[];
  readonly incomingPath: string;
  readonly outgoingPath: string;
}

function path(points: readonly XY[]): string {
  return points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ');
}

/**
 * Pure geometry for the hand-made SVG line chart: both lines share one axis (0 at the bottom, the largest day at the top).
 * It only places the numbers the backend sent; it adds nothing up.
 */
export function buildChart(days: readonly DayPoint[], width: number, height: number): Chart {
  const max = days.reduce((m, d) => Math.max(m, d.incoming, d.outgoing), 0);
  const xAt = (i: number) => (days.length === 1 ? width / 2 : (width * i) / (days.length - 1));
  const yAt = (value: number) => (max === 0 ? height : Math.round((height - (value / max) * height) * 100) / 100);
  const incoming = days.map((d, i) => ({ x: xAt(i), y: yAt(d.incoming) }));
  const outgoing = days.map((d, i) => ({ x: xAt(i), y: yAt(d.outgoing) }));
  return { max, incoming, outgoing, incomingPath: path(incoming), outgoingPath: path(outgoing) };
}

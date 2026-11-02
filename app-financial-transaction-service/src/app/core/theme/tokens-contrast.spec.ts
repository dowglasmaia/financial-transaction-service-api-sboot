import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

type Theme = 'dark' | 'light';

/** Reads the colour tokens of one theme block from src/styles.scss (the single source of the design tokens). */
function readTokens(theme: Theme): Record<string, string> {
  const scss = readFileSync(resolve(process.cwd(), 'src/styles.scss'), 'utf-8');
  const marker = theme === 'dark' ? '/* theme:dark */' : '/* theme:light */';
  const start = scss.indexOf(marker);
  if (start < 0) throw new Error(`theme block not found: ${theme}`);
  const body = scss.slice(scss.indexOf('{', start) + 1, scss.indexOf('}', start));
  const tokens: Record<string, string> = {};
  for (const match of body.matchAll(/--([a-z-]+):\s*(#[0-9a-fA-F]{6})/g)) tokens[match[1]] = match[2];
  return tokens;
}

function luminance(hex: string): number {
  const [r, g, b] = [1, 3, 5]
    .map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

// [foreground, background, minimum ratio]: text >= 4.5, interface chrome >= 3 (WCAG 2.2 AA)
const PAIRS: ReadonlyArray<[string, string, number]> = [
  ['content', 'surface', 4.5],
  ['content', 'surface-raised', 4.5],
  ['content', 'surface-overlay', 4.5],
  ['content-muted', 'surface', 4.5],
  ['content-muted', 'surface-raised', 4.5],
  ['content-muted', 'surface-sunken', 4.5],
  ['accent', 'surface', 4.5],
  ['accent', 'surface-raised', 4.5],
  ['on-accent', 'accent', 4.5],
  ['income', 'surface', 4.5],
  ['income', 'surface-raised', 4.5],
  ['expense', 'surface', 4.5],
  ['expense', 'surface-raised', 4.5],
  ['warning', 'surface-raised', 4.5],
  ['line-input', 'surface', 3],
  ['line-input', 'surface-raised', 3],
  ['line-input', 'surface-sunken', 3],
  ['accent', 'surface-sunken', 3],
];

describe.each<Theme>(['dark', 'light'])('design tokens contrast (%s)', (theme) => {
  const tokens = readTokens(theme);

  it.each(PAIRS)('%s on %s is at least %s:1', (foreground, background, minimum) => {
    expect(tokens[foreground], `missing token ${foreground}`).toBeDefined();
    expect(tokens[background], `missing token ${background}`).toBeDefined();
    expect(contrast(tokens[foreground], tokens[background])).toBeGreaterThanOrEqual(minimum);
  });
});

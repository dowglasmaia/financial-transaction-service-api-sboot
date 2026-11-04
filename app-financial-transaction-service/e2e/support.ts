import AxeBuilder from '@axe-core/playwright';
import { Page, expect } from '@playwright/test';

export const MOCK_IDP = 'http://mock-idp:9099';
export const AUDIENCE = 'https://api.financial-transaction.local';

/** The app reads /config.json at start: point it at the MOCK identity provider (public values only). */
export async function useMockIdp(page: Page): Promise<void> {
  await page.route('**/config.json', (route) =>
    route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({ apiBaseUrl: '/api/v1', demoBanner: true, auth: { domain: MOCK_IDP, clientId: 'e2e-client', audience: AUDIENCE } }),
    }),
  );
}

/** Collects what must never leak: console output and every URL the page visits. */
export function watchLeaks(page: Page): { console: string[]; urls: string[] } {
  const seen = { console: [] as string[], urls: [] as string[] };
  page.on('console', (message) => seen.console.push(message.text()));
  page.on('framenavigated', (frame) => seen.urls.push(frame.url()));
  return seen;
}

/** Signs in through the mock IdP page by choosing one of its users (a button), from the sign-in screen. */
export async function signInAs(page: Page, user: RegExp): Promise<void> {
  await page.goto('/signin');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await page.getByRole('button', { name: user }).click();
}

/** Fails on serious or critical accessibility violations of the current page. */
export async function expectNoSeriousA11yViolations(page: Page): Promise<void> {
  const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
  const serious = result.violations.filter((violation) => violation.impact === 'serious' || violation.impact === 'critical');
  expect(serious.map((v) => `${v.id}: ${v.help} (${v.nodes.length}) e.g. ${v.nodes[0]?.target.join(' ')} — ${v.nodes[0]?.any[0]?.message ?? ''}`)).toEqual([]);
}

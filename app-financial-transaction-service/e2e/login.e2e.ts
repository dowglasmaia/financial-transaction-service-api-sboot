import { expect, test } from '@playwright/test';
import { expectNoSeriousA11yViolations, signInAs, useMockIdp, watchLeaks } from './support';

/** A real token: three base64url segments, the first two JSON (`eyJ…`). The SDK's harmless telemetry param also starts with eyJ, so the shape matters. */
const JWT = /eyJ[\w-]+\.eyJ[\w-]+\.[\w-]+/;
const TOKEN_PARAM = /[?&#](access_token|id_token|refresh_token)=/;

test.beforeEach(async ({ page }) => {
  await useMockIdp(page);
});

test.describe('unauthenticated', () => {
  test('/home redirects to the landing page', async ({ page }) => {
    await page.goto('/home');
    await expect(page).toHaveURL('/');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Your financial life, simpler and safer.');
    await expect(page.getByRole('navigation', { name: 'Main' })).toHaveCount(0);
  });

  test('landing and sign-in have no serious accessibility violations', async ({ page }) => {
    await page.goto('/');
    await expectNoSeriousA11yViolations(page);
    await page.goto('/signin');
    await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
    await expectNoSeriousA11yViolations(page);
  });
});

test.describe('sign in with the mock IdP', () => {
  test('user 1 lands on Home with the demo account and the balance from the API', async ({ page }) => {
    const leaks = watchLeaks(page);
    await signInAs(page, /Maria Silva - demo customer/);

    await expect(page).toHaveURL('/home');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Hello, Maria!');
    await expect(page.getByText('Agency 0211 · Account 2022')).toBeVisible();
    await expect(page.locator('app-balance-card .sr-only')).toContainText('R$ 5,000.00');
    await expectNoSeriousA11yViolations(page);

    // tokens never leak: not in a URL, not in the console, not in web storage
    expect(leaks.urls.filter((url) => JWT.test(url) || TOKEN_PARAM.test(url))).toEqual([]);
    expect(leaks.console.filter((line) => JWT.test(line))).toEqual([]);
    const storage = await page.evaluate(() => JSON.stringify({ ...localStorage }) + JSON.stringify({ ...sessionStorage }));
    expect(storage).not.toMatch(JWT);
  });

  test('user 2 sees a different account and balance', async ({ page }) => {
    await signInAs(page, /João Santos - second customer/);

    await expect(page.getByRole('heading', { level: 1 })).toContainText('Hello, João!');
    await expect(page.getByText('Agency 0211 · Account 3033')).toBeVisible();
    await expect(page.locator('app-balance-card .sr-only')).toContainText('R$ 1,000.00');
  });

  test('a valid sign-in that is not linked to a customer gets the "no access" page', async ({ page }) => {
    await signInAs(page, /Carla Souza - valid sign-in/);

    await expect(page).toHaveURL('/no-access');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('No access to this app yet');
  });

  test('sign out asks for confirmation and returns to the landing page', async ({ page }) => {
    await signInAs(page, /Maria Silva - demo customer/);
    await expect(page).toHaveURL('/home');

    await page.getByRole('button', { name: /Maria Silva|Account menu for Maria Silva/ }).first().click();
    await page.getByRole('button', { name: 'Sign out' }).click();
    await expect(page.getByRole('dialog')).toContainText('Sign out of Maia Bank?');
    await page.getByRole('dialog').getByRole('button', { name: 'Sign out' }).click();

    await expect(page).toHaveURL('/');
    await page.goto('/home');
    await expect(page).toHaveURL('/'); // the session is really gone
  });

  test('an expired access token shows the session-expired dialog (one renewal, then the dialog)', async ({ page }) => {
    await signInAs(page, /Maria Silva - access tokens already expired/);

    await expect(page.getByRole('dialog')).toContainText('Your session expired');
    await page.getByRole('dialog').getByRole('button', { name: 'Sign in again' }).click();
    await expect(page.getByRole('button', { name: /Maria Silva - demo customer/ })).toBeVisible(); // back at the identity provider
  });
});

import { execFileSync } from 'node:child_process';
import { Page, expect, test } from '@playwright/test';
import { expectNoSeriousA11yViolations, signInAs, useMockIdp } from './support';

/**
 * SL-02 on the real chain. The ledger rows are written straight into PostgreSQL (there is no screen that creates money yet),
 * for the two mock customers only; balances are not touched. Every figure asserted in the browser is the backend's.
 */
const MARIA = /Maria Silva - demo customer/;
const JOAO = /João Santos - second customer/;
const DB = ['exec', 'pg-db', 'psql', '-U', 'transactions', '-d', 'transactions', '-At', '-c'];

function sql(statement: string): string {
  // psql prints the command tag ("INSERT 0 1") after a RETURNING row: the first line is the value
  return execFileSync('docker', [...DB, statement], { encoding: 'utf8' }).trim().split(/\r?\n/)[0];
}

let foreignId = '';
let ownId = '';

test.beforeAll(() => {
  const accounts = ['2022', '3033'].map((number) => sql(`select id || ':' || number from account where number = '${number}'`));
  const idOf = (number: string) => accounts.find((line) => line.endsWith(`:${number}`))!.split(':')[0];
  const [maria, joao] = [idOf('2022'), idOf('3033')];
  sql(`delete from transaction where account_id in ('${maria}','${joao}')`);
  const agency = sql(`select agency_id from account where id = '${maria}'`);
  const insert = (account: string, type: string, amount: string, at: string) =>
    sql(`insert into transaction (id, account_id, agency_id, transaction_type, amount, status, date_hour) values (gen_random_uuid()::text, '${account}', '${agency}', '${type}', ${amount}, 'COMPLETED', ${at}) returning id`);

  // one fixed day (noon in Sao Paulo) for the single-day filter, far enough in the past to stay out of the presets
  ownId = insert(maria, 'PIX', '12.50', "timestamptz '2026-03-11 12:00:00-03'");
  insert(maria, 'DEPOSIT', '1.00', "timestamptz '2026-03-10 23:59:59-03'"); // the day before: out of the single day
  insert(maria, 'DEPOSIT', '1.00', "timestamptz '2026-03-12 00:00:00-03'"); // the day after: out
  // recent activity: 3 outgoing types and 12 small deposits, so the type chip and the second page have something to do
  insert(maria, 'DEPOSIT', '100.00', "now() - interval '3 days'");
  insert(maria, 'PIX', '30.00', "now() - interval '2 days'");
  insert(maria, 'WITHDRAW', '20.00', "now() - interval '2 days'");
  insert(maria, 'PHONE_RECHARGE', '5.00', "now() - interval '1 day'");
  for (let i = 0; i < 12; i++) insert(maria, 'DEPOSIT', '1.00', `now() - interval '${i + 1} hours'`);
  foreignId = insert(joao, 'DEPOSIT', '77.00', "now() - interval '1 day'");
});

test.beforeEach(async ({ page }) => {
  await useMockIdp(page);
});

/** Client-side navigation: the access token lives in memory, so a full page load would sign the customer out. */
async function goInApp(page: Page, url: string): Promise<void> {
  await page.evaluate((target) => {
    history.pushState({}, '', target);
    dispatchEvent(new PopStateEvent('popstate'));
  }, url);
}

/** Signs in and waits for Home: the callback must finish before the test moves around inside the app. */
async function signedInAs(page: Page, user: RegExp, firstName: string): Promise<void> {
  await signInAs(page, user);
  await expect(page.getByRole('heading', { level: 1 })).toContainText(`Hello, ${firstName}!`);
}

const incomingTile = (page: Page) => page.getByLabel('Totals of the period').getByText(/^R\$ [\d,]+\.\d{2}$/).first();

test.describe('statement, detail and Home summary', () => {
  test('a single custom day shows exactly that day (Sao Paulo), with the backend totals', async ({ page }) => {
    await signedInAs(page, MARIA, 'Maria');
    await goInApp(page, '/transactions?period=custom&from=2026-03-11&to=2026-03-11');

    await expect(page.getByText('Showing 1–1 of 1')).toBeVisible();
    await expect(page.getByLabel('Totals of the period')).toContainText('R$ 12.50'); // outgoing: the Pix of that day only
    await expectNoSeriousA11yViolations(page);
  });

  test('type chip, sort and pagination work together and live in the URL', async ({ page }) => {
    await signedInAs(page, MARIA, 'Maria');
    await goInApp(page, '/transactions');
    await expect(page.getByText(/Showing 1–10 of \d+/)).toBeVisible();

    await page.getByRole('group', { name: 'Type' }).getByRole('button', { name: 'Deposit' }).click();
    await expect(page).toHaveURL(/type=DEPOSIT/);
    await expect(page.getByText(/Showing 1–10 of 13/)).toBeVisible(); // 1 + 12 deposits in the last 30 days

    await page.getByLabel('Sort by').selectOption('amount-desc');
    await expect(page).toHaveURL(/sort=amount/);

    await page.getByRole('navigation', { name: 'Pages' }).getByRole('button', { name: 'Next' }).click();
    await expect(page).toHaveURL(/page=1/);
    await expect(page.getByText('Showing 11–13 of 13')).toBeVisible();
    // the totals ignore the filter and the page: the Pix and the withdrawal are still counted as outgoing
    await expect(page.getByLabel('Totals of the period')).toContainText('R$ 55.00');
  });

  test('a row opens its detail by id, with the id copied on request', async ({ page, context }) => {
    await context.grantPermissions(['clipboard-read', 'clipboard-write']);
    await signedInAs(page, MARIA, 'Maria');
    await goInApp(page, '/transactions?period=custom&from=2026-03-11&to=2026-03-11');
    await page.getByRole('link', { name: /Pix sent/ }).first().click();

    await expect(page).toHaveURL(new RegExp(`/transactions/${ownId}$`));
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Pix sent');
    await expect(page.getByText('− R$ 12.50')).toBeVisible();
    await expect(page.getByText('Registered')).toBeVisible();
    await expectNoSeriousA11yViolations(page);

    await page.getByRole('button', { name: 'Copy transaction id' }).click();
    await expect(page.getByText('Transaction id copied')).toBeVisible();
  });

  test("user 2 opening user 1's transaction gets the not-found state, never its data", async ({ page }) => {
    await signedInAs(page, JOAO, 'João');
    await goInApp(page, `/transactions/${ownId}`);

    await expect(page.getByText('Transaction not found').first()).toBeVisible();
    await expect(page.getByText('R$ 12.50')).toHaveCount(0);

    // and user 2's own statement holds only user 2's row
    await goInApp(page, '/transactions');
    await expect(page.getByText('Showing 1–1 of 1')).toBeVisible();
    await page.locator('main').getByRole('link', { name: /Deposit/ }).first().click();
    await expect(page).toHaveURL(new RegExp(`/transactions/${foreignId}$`));
  });

  test('Home shows the same totals as the Statement for the same period, a chart with a table fallback and recent rows', async ({ page }) => {
    await signedInAs(page, MARIA, 'Maria');
    await expect(page.getByRole('heading', { name: 'Your activity' })).toBeVisible();
    await expect(page.getByRole('group', { name: 'Period' }).getByRole('button', { name: '30 days' })).toHaveAttribute('aria-pressed', 'true');
    const homeTotals = await page.getByLabel('Totals of the period').innerText();
    await expect(page.locator('app-line-chart svg')).toBeVisible();
    await expectNoSeriousA11yViolations(page);

    await page.getByRole('button', { name: 'View as table' }).click();
    await expect(page.locator('app-line-chart table')).toBeVisible();
    await expectNoSeriousA11yViolations(page);

    await expect(page.getByRole('list', { name: 'Recent transactions' }).getByRole('link')).toHaveCount(5);
    await page.getByRole('link', { name: /See all/ }).click();
    await expect(page.getByLabel('Totals of the period')).toBeVisible();
    expect(await page.getByLabel('Totals of the period').innerText()).toBe(homeTotals);
    await expect(incomingTile(page)).toBeVisible();
    await expectNoSeriousA11yViolations(page);
  });
});

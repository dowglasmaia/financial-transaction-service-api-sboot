import { execFileSync } from 'node:child_process';
import { Page, expect, test } from '@playwright/test';
import { expectNoSeriousA11yViolations, signInAs, useMockIdp } from './support';

/**
 * SL-04 (Deposit) on the real chain: generate the DEMO boleto, simulate the payment, and check the money in PostgreSQL as
 * well as on screen. Maria (account 2022) starts each test with 5,000.00 and an empty ledger.
 */
const MARIA = /Maria Silva - demo customer/;
const DB = ['exec', 'pg-db', 'psql', '-U', 'transactions', '-d', 'transactions', '-At', '-c'];
const ENDPOINT = '**/api/v1/transactions/deposit';

function sql(statement: string): string {
  return execFileSync('docker', [...DB, statement], { encoding: 'utf8' })
    .trim()
    .split(/\r?\n/)[0];
}

const balance = () => Number(sql("select balance from account where number = '2022'"));
const depositRows = () =>
  Number(
    sql(
      "select count(*) from transaction t join account a on a.id = t.account_id where a.number = '2022' and t.transaction_type = 'DEPOSIT'",
    ),
  );

test.beforeEach(async ({ page }) => {
  sql("delete from transaction where account_id = (select id from account where number = '2022')");
  sql("update account set balance = 5000.00 where number = '2022'");
  sql('delete from idempotency_key');
  await useMockIdp(page);
});

/** Client-side navigation: the access token lives in memory, so a full page load would sign the customer out. */
async function goInApp(page: Page, url: string): Promise<void> {
  await page.evaluate((target) => {
    history.pushState({}, '', target);
    dispatchEvent(new PopStateEvent('popstate'));
  }, url);
}

async function openDeposit(page: Page): Promise<void> {
  await signInAs(page, MARIA);
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Hello, Maria!');
  await goInApp(page, '/deposit');
  await expect(page.getByRole('heading', { level: 1, name: 'Deposit' })).toBeVisible();
}

async function generate(page: Page, amount: string): Promise<void> {
  await page.getByLabel('Deposit amount').fill(amount);
  await page.getByRole('button', { name: 'Generate boleto' }).click();
  await expect(page.getByRole('button', { name: 'Simulate payment' })).toBeVisible();
}

test.describe('deposit', () => {
  test('generate a demo boleto and simulate the payment: balance +250 exactly once and a DEPOSIT row', async ({
    page,
  }) => {
    const posts: { body: string | null; key: string | undefined }[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('/transactions/deposit'))
        posts.push({ body: request.postData(), key: request.headers()['idempotency-key'] });
    });
    await openDeposit(page);
    await expect(
      page.getByText('Preview. The barcode and the data appear after you generate the boleto.'),
    ).toBeVisible();
    await expectNoSeriousA11yViolations(page);

    await page.getByRole('button', { name: 'R$ 250', exact: true }).click();
    await page.getByRole('button', { name: 'Generate boleto' }).click();
    await expect(page.getByLabel('Demo boleto')).toContainText('Maia Bank (demo)');
    await expect(page.getByLabel('Demo boleto')).toContainText('Maria Silva');
    await expect(page.getByLabel('Demo boleto')).toContainText('R$ 250.00');
    await expect(page.getByText('Demo. This is not a real boleto.')).toBeVisible();
    await page.waitForTimeout(500);
    await expectNoSeriousA11yViolations(page); // the boleto card

    await page.getByRole('button', { name: 'Copy line' }).click();
    await expect(page.getByText('Demo action')).toBeVisible();
    expect(posts).toHaveLength(0); // the demo actions send nothing

    await page.getByRole('button', { name: 'Simulate payment' }).dblclick(); // a double click is still ONE request
    await expect(page.getByRole('heading', { level: 1, name: 'Deposit registered' })).toBeVisible();
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page); // the result panel

    expect(posts).toHaveLength(1);
    expect(JSON.parse(posts[0].body!)).toEqual({ agency: '0211', account: '2022', amount: 250 });
    expect(posts[0].key).toMatch(/^[0-9a-f-]{36}$/);
    expect(balance()).toBe(5250);
    expect(depositRows()).toBe(1);

    await page.getByRole('link', { name: 'View transactions' }).click();
    await expect(page.locator('main').getByRole('link', { name: 'Deposit' }).first()).toBeVisible();
    await expect(page.getByLabel('Totals of the period')).toContainText('R$ 5,250.00');
  });

  test('zero cannot generate a boleto and says why', async ({ page }) => {
    await openDeposit(page);
    await page.getByLabel('Deposit amount').fill('0');
    await expect(page.getByText('Enter an amount greater than zero.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Generate boleto' })).toBeDisabled();
  });

  test('"New amount" goes back to the form and nothing was deposited', async ({ page }) => {
    await openDeposit(page);
    await generate(page, '100');
    await page.getByRole('button', { name: 'New amount' }).click();
    await expect(page.getByRole('button', { name: 'Generate boleto' })).toBeVisible();
    expect(balance()).toBe(5000);
    expect(depositRows()).toBe(0);
  });

  test('a lost answer is uncertain; "Try again" reuses the key and the account is credited once', async ({
    page,
  }) => {
    const keys: (string | undefined)[] = [];
    let first = true;
    await page.route(ENDPOINT, async (route) => {
      keys.push(route.request().headers()['idempotency-key']);
      if (first) {
        first = false;
        await route.fetch(); // the server receives and processes it...
        await route.abort('failed'); // ...but the answer never reaches the browser
      } else {
        await route.continue();
      }
    });
    await openDeposit(page);
    await generate(page, '100');
    await page.getByRole('button', { name: 'Simulate payment' }).click();

    await expect(
      page.getByRole('heading', { level: 1, name: 'Outcome not confirmed' }),
    ).toBeVisible();
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page);
    expect(balance()).toBe(5100); // the first attempt did go through on the server
    expect(keys).toHaveLength(1);

    await page.getByRole('button', { name: 'Try again' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Deposit registered' })).toBeVisible();
    expect(keys).toHaveLength(2);
    expect(keys[1]).toBe(keys[0]);
    expect(balance()).toBe(5100); // credited ONCE
    expect(depositRows()).toBe(1);
  });

  test('a request that never leaves (network down) is uncertain and is not retried by itself', async ({
    page,
  }) => {
    let sent = 0;
    await page.route(ENDPOINT, async (route) => {
      sent++;
      await route.abort('internetdisconnected');
    });
    await openDeposit(page);
    await generate(page, '50');
    await page.getByRole('button', { name: 'Simulate payment' }).click();

    await expect(
      page.getByRole('heading', { level: 1, name: 'Outcome not confirmed' }),
    ).toBeVisible();
    await page.waitForTimeout(1500);
    expect(sent).toBe(1);
    expect(balance()).toBe(5000);
  });

  test('a 404 from the server is shown as a field error on the amount', async ({ page }) => {
    await page.route(ENDPOINT, (route) =>
      route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'NOT_FOUND', message: 'Account not found.' }),
      }),
    );
    await openDeposit(page);
    await generate(page, '100');
    await page.getByRole('button', { name: 'Simulate payment' }).click();

    await expect(page.getByRole('alert').filter({ hasText: 'Account not found.' })).toBeVisible();
    await expect(page.getByLabel('Deposit amount')).toHaveValue('100.00');
    expect(balance()).toBe(5000);
  });
});

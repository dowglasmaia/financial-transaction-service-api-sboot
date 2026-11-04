import { execFileSync } from 'node:child_process';
import { Page, expect, test } from '@playwright/test';
import { expectNoSeriousA11yViolations, signInAs, useMockIdp } from './support';

/**
 * SL-03 on the real chain: withdrawal, the review dialog, the three result panels and the Idempotency-Key replay.
 * The balance and the ledger rows are checked in PostgreSQL as well as on screen. Maria (account 2022) starts each test
 * with 5,000.00 and an empty ledger.
 */
const MARIA = /Maria Silva - demo customer/;
const DB = ['exec', 'pg-db', 'psql', '-U', 'transactions', '-d', 'transactions', '-At', '-c'];
const ENDPOINT = '**/api/v1/transactions/atm-withdrawal';
const INSUFFICIENT = 'Operation not performed, there is not enough balance in the account.';

function sql(statement: string): string {
  return execFileSync('docker', [...DB, statement], { encoding: 'utf8' }).trim().split(/\r?\n/)[0];
}

const balance = () => Number(sql("select balance from account where number = '2022'"));
const withdrawRows = () => Number(sql("select count(*) from transaction t join account a on a.id = t.account_id where a.number = '2022' and t.transaction_type = 'WITHDRAW'"));

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

async function openWithdraw(page: Page): Promise<void> {
  await signInAs(page, MARIA);
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Hello, Maria!');
  await goInApp(page, '/withdraw');
  await expect(page.getByRole('heading', { level: 1, name: 'Withdraw' })).toBeVisible();
  await expect(page.getByText('R$ 5,000.00').first()).toBeVisible(); // the balance comes from the API
}

async function review(page: Page, amount: string): Promise<void> {
  await page.getByLabel('Withdrawal amount').fill(amount);
  await page.getByRole('button', { name: 'Review withdrawal' }).click();
  await expect(page.getByRole('dialog', { name: 'Review withdrawal' })).toBeVisible();
}

const confirm = (page: Page) => page.getByRole('dialog').getByRole('button', { name: 'Confirm' });

test.describe('withdrawal', () => {
  test('withdraw 100: one request with the body {amount} only, the balance goes down by 100 and a row appears', async ({ page }) => {
    const posts: { body: string | null; key: string | undefined }[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('atm-withdrawal')) posts.push({ body: request.postData(), key: request.headers()['idempotency-key'] });
    });
    await openWithdraw(page);
    await expectNoSeriousA11yViolations(page);

    await page.getByRole('button', { name: 'R$ 100', exact: true }).click();
    await page.getByRole('button', { name: 'Review withdrawal' }).click();
    await expect(page.getByRole('dialog', { name: 'Review withdrawal' })).toContainText('R$ 100.00');
    await page.waitForTimeout(500); // let the entrance animation finish: axe would read half-transparent colours
    await expectNoSeriousA11yViolations(page); // the dialog

    await confirm(page).dblclick(); // a double click must still be ONE request
    await expect(page.getByRole('heading', { level: 1, name: 'Withdrawal registered' })).toBeVisible();
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page); // the result panel

    expect(posts).toHaveLength(1);
    expect(JSON.parse(posts[0].body!)).toEqual({ amount: 100 });
    expect(posts[0].key).toMatch(/^[0-9a-f-]{36}$/);
    expect(balance()).toBe(4900);
    expect(withdrawRows()).toBe(1);

    await page.getByRole('link', { name: 'View transactions' }).click();
    await expect(page.getByRole('link', { name: 'ATM withdrawal' }).first()).toBeVisible();
    await expect(page.getByLabel('Totals of the period')).toContainText('R$ 4,900.00');
  });

  test('the exact full balance is accepted and leaves the balance at zero', async ({ page }) => {
    await openWithdraw(page);
    await page.getByRole('button', { name: 'Use full balance' }).click();
    await expect(page.getByLabel('Withdrawal amount')).toHaveValue('5000.00');
    await page.getByRole('button', { name: 'Review withdrawal' }).click();
    await confirm(page).click();

    await expect(page.getByRole('heading', { level: 1, name: 'Withdrawal registered' })).toBeVisible();
    expect(balance()).toBe(0);
    expect(withdrawRows()).toBe(1);
  });

  test('an amount above the balance is refused by the server: business-failure panel, balance unchanged', async ({ page }) => {
    await openWithdraw(page);
    await page.getByLabel('Withdrawal amount').fill('5000.01');
    await expect(page.getByText("The amount can't exceed your available balance.")).toBeVisible(); // feedback only
    await review(page, '5000.01');
    await confirm(page).click();

    await expect(page.getByRole('heading', { level: 1, name: "Couldn't complete the withdrawal" })).toBeVisible();
    await expect(page.locator('app-result-panel')).toContainText(INSUFFICIENT);
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page);
    expect(balance()).toBe(5000);
    expect(withdrawRows()).toBe(0);

    await page.getByRole('button', { name: 'Edit and try again' }).click(); // back to the form, nothing lost
    await expect(page.getByLabel('Withdrawal amount')).toHaveValue('5000.01');
  });

  test('a lost answer is an uncertain outcome; "Try again" reuses the key and the backend debits once', async ({ page }) => {
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
    await openWithdraw(page);
    await review(page, '100');
    await confirm(page).click();

    await expect(page.getByRole('heading', { level: 1, name: "We couldn't confirm the operation" })).toBeVisible();
    await expect(page.locator('app-result-panel')).toContainText("Check your transactions before trying again, so you don't pay twice.");
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page);
    expect(balance()).toBe(4900); // the first attempt did go through on the server
    expect(keys).toHaveLength(1);

    await page.getByRole('button', { name: 'Try again' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Withdrawal registered' })).toBeVisible();

    expect(keys).toHaveLength(2);
    expect(keys[1]).toBe(keys[0]); // the same Idempotency-Key
    expect(balance()).toBe(4900); // debited ONCE
    expect(withdrawRows()).toBe(1);
  });

  test('a request that never leaves (network down) is uncertain, is not retried by itself, and sends nothing twice', async ({ page }) => {
    let sent = 0;
    await page.route(ENDPOINT, async (route) => {
      sent++;
      await route.abort('internetdisconnected');
    });
    await openWithdraw(page);
    await review(page, '50');
    await confirm(page).click();

    await expect(page.getByRole('heading', { level: 1, name: "We couldn't confirm the operation" })).toBeVisible();
    await page.waitForTimeout(1500); // no automatic retry, whatever the wait
    expect(sent).toBe(1);
    expect(balance()).toBe(5000);
    await expect(page.getByRole('link', { name: 'View transactions' })).toBeVisible();
  });
});

import { execFileSync } from 'node:child_process';
import { Page, expect, test } from '@playwright/test';
import { expectNoSeriousA11yViolations, signInAs, useMockIdp } from './support';

/**
 * SL-04 (Recharge) on the real chain: carrier, masked number, review, the "requested" copy, and the debit + ledger row +
 * outbox event checked in PostgreSQL. Maria (account 2022) starts each test with 5,000.00 and an empty ledger.
 */
const MARIA = /Maria Silva - demo customer/;
const DB = ['exec', 'pg-db', 'psql', '-U', 'transactions', '-d', 'transactions', '-At', '-c'];
const ENDPOINT = '**/api/v1/transactions/recharge-mobile';
const SUCCESS = 'Recharge requested. Your carrier will apply the credit shortly.';

function sql(statement: string): string {
  return execFileSync('docker', [...DB, statement], { encoding: 'utf8' })
    .trim()
    .split(/\r?\n/)[0];
}

const balance = () => Number(sql("select balance from account where number = '2022'"));
const rechargeRows = () =>
  Number(
    sql(
      "select count(*) from transaction t join account a on a.id = t.account_id where a.number = '2022' and t.transaction_type = 'PHONE_RECHARGE'",
    ),
  );
const outboxRows = () =>
  Number(
    sql(
      "select count(*) from outbox_event where topic = 'recharge-mobile-topic' and payload like '%\"areaCode\":11%'",
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

async function openRecharge(page: Page): Promise<void> {
  await signInAs(page, MARIA);
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Hello, Maria!');
  await goInApp(page, '/recharge');
  await expect(page.getByRole('heading', { level: 1, name: 'Recharge' })).toBeVisible();
  await expect(page.getByText('R$ 5,000.00').first()).toBeVisible();
}

async function fill(page: Page, carrier: string, digits: string, amount: string): Promise<void> {
  await page.getByRole('button', { name: carrier, exact: true }).click();
  await page.getByLabel('Mobile number').fill(digits);
  await page.getByLabel('Recharge amount').fill(amount);
}

async function review(
  page: Page,
  carrier = 'Vivo',
  digits = '11912345678',
  amount = '30',
): Promise<void> {
  await fill(page, carrier, digits, amount);
  await page.getByRole('button', { name: 'Review recharge' }).click();
  await expect(page.getByRole('dialog', { name: 'Review recharge' })).toBeVisible();
}

const confirm = (page: Page) => page.getByRole('dialog').getByRole('button', { name: 'Confirm' });

test.describe('recharge', () => {
  test('recharge 30 on Vivo: one request split into areaCode + phoneNumber, balance −30, a PHONE_RECHARGE row, an outbox event, "requested" and never "completed"', async ({
    page,
  }) => {
    const posts: { body: string | null; key: string | undefined }[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('recharge-mobile'))
        posts.push({ body: request.postData(), key: request.headers()['idempotency-key'] });
    });
    const outboxBefore = outboxRows();
    await openRecharge(page);
    await expectNoSeriousA11yViolations(page);

    await page.getByRole('button', { name: 'Vivo', exact: true }).click();
    await page.getByLabel('Mobile number').fill('11912345678');
    await expect(page.getByLabel('Mobile number')).toHaveValue('(11) 9 1234-5678');
    await page.getByRole('button', { name: 'R$ 30', exact: true }).click();
    await page.waitForTimeout(300);
    await expectNoSeriousA11yViolations(page); // the filled form

    await page.getByRole('button', { name: 'Review recharge' }).click();
    const dialog = page.getByRole('dialog', { name: 'Review recharge' });
    await expect(dialog).toContainText('Vivo');
    await expect(dialog).toContainText('(11) 9 1234-5678');
    await expect(dialog).toContainText('R$ 30.00');
    await page.waitForTimeout(500);
    await expectNoSeriousA11yViolations(page); // the dialog

    await confirm(page).dblclick(); // still ONE request
    await expect(page.getByRole('heading', { level: 1, name: 'Recharge requested' })).toBeVisible();
    await expect(page.locator('app-result-panel')).toContainText(SUCCESS);
    expect(await page.locator('body').innerText()).not.toMatch(/completed/i);
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page); // the result panel

    expect(posts).toHaveLength(1);
    expect(JSON.parse(posts[0].body!)).toEqual({
      areaCode: 11,
      phoneNumber: 912345678,
      carrier: 'VIVO',
      amount: 30,
    });
    expect(posts[0].key).toMatch(/^[0-9a-f-]{36}$/);
    expect(balance()).toBe(4970);
    expect(rechargeRows()).toBe(1);
    expect(outboxRows()).toBe(outboxBefore + 1); // exactly one event for the one recharge

    await page.getByRole('link', { name: 'View transactions' }).click();
    await expect(
      page.locator('main').getByRole('link', { name: 'Mobile recharge' }).first(),
    ).toBeVisible();
  });

  test('an invalid number or a missing amount keeps Review disabled and explains the number', async ({
    page,
  }) => {
    await openRecharge(page);
    const reviewButton = page.getByRole('button', { name: 'Review recharge' });
    await expect(reviewButton).toBeDisabled();

    await page.getByRole('button', { name: 'TIM', exact: true }).click();
    await page.getByLabel('Mobile number').fill('11812345678'); // the 9 of a mobile is missing
    await page.getByLabel('Recharge amount').fill('20');
    await expect(
      page.getByText('Enter a valid mobile number, for example (11) 9 1234-5678.'),
    ).toBeVisible();
    await expect(reviewButton).toBeDisabled();

    await page.getByLabel('Mobile number').fill('11912345678');
    await expect(reviewButton).toBeEnabled();
    await page.getByLabel('Recharge amount').fill('0');
    await expect(page.getByText('Enter an amount greater than zero.')).toBeVisible();
    await expect(reviewButton).toBeDisabled();
    expect(rechargeRows()).toBe(0);
  });

  test('an amount above the balance is refused by the server: business panel, nothing charged, nothing published', async ({
    page,
  }) => {
    await openRecharge(page);
    await review(page, 'Claro', '21987654321', '5000.01');
    await confirm(page).click();

    await expect(
      page.getByRole('heading', { level: 1, name: "Couldn't request the recharge" }),
    ).toBeVisible();
    await expect(page.locator('app-result-panel')).toContainText(
      'Operation not performed, there is not enough balance in the account.',
    );
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page);
    expect(balance()).toBe(5000);
    expect(rechargeRows()).toBe(0);

    await page.getByRole('button', { name: 'Edit and try again' }).click();
    await expect(page.getByLabel('Mobile number')).toHaveValue('(21) 9 8765-4321');
  });

  test('a lost answer is uncertain; "Try again" reuses the key and the debit happens once', async ({
    page,
  }) => {
    const keys: (string | undefined)[] = [];
    let first = true;
    await page.route(ENDPOINT, async (route) => {
      keys.push(route.request().headers()['idempotency-key']);
      if (first) {
        first = false;
        await route.fetch();
        await route.abort('failed');
      } else {
        await route.continue();
      }
    });
    await openRecharge(page);
    await review(page, 'Algar', '11912345678', '15');
    await confirm(page).click();

    await expect(
      page.getByRole('heading', { level: 1, name: 'Outcome not confirmed' }),
    ).toBeVisible();
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page);
    expect(balance()).toBe(4985);

    await page.getByRole('button', { name: 'Try again' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Recharge requested' })).toBeVisible();
    expect(keys).toHaveLength(2);
    expect(keys[1]).toBe(keys[0]);
    expect(balance()).toBe(4985);
    expect(rechargeRows()).toBe(1);
  });

  test('a request that never leaves (network down) is uncertain and is not retried by itself', async ({
    page,
  }) => {
    let sent = 0;
    await page.route(ENDPOINT, async (route) => {
      sent++;
      await route.abort('internetdisconnected');
    });
    await openRecharge(page);
    await review(page);
    await confirm(page).click();

    await expect(
      page.getByRole('heading', { level: 1, name: 'Outcome not confirmed' }),
    ).toBeVisible();
    await page.waitForTimeout(1500);
    expect(sent).toBe(1);
    expect(balance()).toBe(5000);
  });
});

test.describe('home', () => {
  test('the Deposit tile opens the Deposit page and the quick-recharge card opens the same review on the Recharge page', async ({
    page,
  }) => {
    await signInAs(page, MARIA);
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Hello, Maria!');
    await expect(
      page
        .locator('main')
        .getByRole('link', { name: /Deposit/ })
        .first(),
    ).toBeVisible();
    await page.waitForTimeout(500);
    await expectNoSeriousA11yViolations(page);

    const card = page.getByLabel('Quick recharge');
    await card.getByRole('button', { name: 'TIM', exact: true }).click();
    await card.getByLabel('Mobile number').fill('21987654321');
    await card.getByLabel('Recharge amount').fill('50');
    await card.getByRole('button', { name: 'Review recharge' }).click();

    await expect(page).toHaveURL(/\/recharge$/);
    const dialog = page.getByRole('dialog', { name: 'Review recharge' });
    await expect(dialog).toContainText('TIM');
    await expect(dialog).toContainText('(21) 9 8765-4321');
    await expect(dialog).toContainText('R$ 50.00');
    expect(rechargeRows()).toBe(0); // Home sent nothing
  });
});

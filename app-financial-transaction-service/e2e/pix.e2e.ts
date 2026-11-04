import { execFileSync } from 'node:child_process';
import { Page, expect, test } from '@playwright/test';
import { expectNoSeriousA11yViolations, signInAs, useMockIdp } from './support';

/**
 * SL-06 (Pix) on the real chain, including the real `maiabank-api` container: three steps, the key rules, the masked key,
 * the three results and the Idempotency-Key replay. The balance and the ledger are checked in PostgreSQL as well as on
 * screen. Maria (account 2022) starts each test with 5,000.00 and an empty ledger.
 */
const MARIA = /Maria Silva - demo customer/;
const DB = ['exec', 'pg-db', 'psql', '-U', 'transactions', '-d', 'transactions', '-At', '-c'];
const ENDPOINT = '**/api/v1/transactions/send-pix';
const CPF = '529.982.247-25';
const NOT_SENT = "We couldn't send this Pix. Your balance was not changed.";

function sql(statement: string): string {
  return execFileSync('docker', [...DB, statement], { encoding: 'utf8' })
    .trim()
    .split(/\r?\n/)[0];
}

const balance = () => Number(sql("select balance from account where number = '2022'"));
const pixRows = () =>
  Number(
    sql(
      "select count(*) from transaction t join account a on a.id = t.account_id where a.number = '2022' and t.transaction_type = 'PIX'",
    ),
  );
const pixStatus = () =>
  sql(
    "select string_agg(t.status::text, ',') from transaction t join account a on a.id = t.account_id where a.number = '2022' and t.transaction_type = 'PIX'",
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

async function openPix(page: Page): Promise<void> {
  await signInAs(page, MARIA);
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Hello, Maria!');
  await goInApp(page, '/pix');
  await expect(page.getByRole('heading', { level: 1, name: 'Pix' })).toBeVisible();
  await expect(page.getByText('R$ 5,000.00').first()).toBeVisible();
}

const next = (page: Page) => page.getByRole('button', { name: 'Continue' });

async function toReview(page: Page, amount: string, key = CPF): Promise<void> {
  await page.getByLabel('Pix key').fill(key);
  await next(page).click();
  await page.getByLabel('Pix amount').fill(amount);
  await next(page).click();
  await expect(page.getByRole('button', { name: 'Confirm and send' })).toBeVisible();
}

test.describe('pix', () => {
  test('send 50 by CPF: three steps, a masked review, ONE request with the plain key, balance −50, a COMPLETED PIX row', async ({
    page,
  }) => {
    const posts: { body: string | null; key: string | undefined }[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('send-pix'))
        posts.push({ body: request.postData(), key: request.headers()['idempotency-key'] });
    });
    await openPix(page);
    await expect(page.getByText('Step 1 of 3')).toBeAttached();
    await expect(page.getByText('Check the key with the person you are paying.')).toBeVisible();
    await expectNoSeriousA11yViolations(page); // step 1

    await page.getByLabel('Pix key').fill('52998224725');
    await expect(page.getByLabel('Pix key')).toHaveValue(CPF);
    await next(page).click();
    await expect(page.getByText('Step 2 of 3')).toBeAttached();
    await page.getByRole('button', { name: 'R$ 50', exact: true }).click();
    await page.waitForTimeout(300);
    await expectNoSeriousA11yViolations(page); // step 2
    await next(page).click();

    await expect(page.getByText('Step 3 of 3')).toBeAttached();
    await expect(page.getByText('•••.982.•••-25')).toBeVisible();
    expect(await page.locator('main').innerText()).not.toContain('52998224725');
    await expectNoSeriousA11yViolations(page); // step 3 (review)

    await page.getByRole('button', { name: 'Confirm and send' }).dblclick(); // still ONE request
    await expect(page.getByRole('heading', { level: 1, name: 'Pix sent' })).toBeVisible();
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page); // the result panel
    expect(await page.locator('main').innerText()).not.toContain('52998224725');

    expect(posts).toHaveLength(1);
    expect(JSON.parse(posts[0].body!)).toEqual({
      pixKey: '52998224725',
      keyType: 'cpf',
      transactionAmount: 50,
    });
    expect(posts[0].key).toMatch(/^[0-9a-f-]{36}$/);
    expect(balance()).toBe(4950);
    expect(pixRows()).toBe(1);
    expect(pixStatus()).toBe('COMPLETED');

    await page.getByRole('link', { name: 'View transactions' }).click();
    await expect(
      page
        .locator('main')
        .getByRole('link', { name: /Pix sent/ })
        .first(),
    ).toBeVisible();
  });

  test('an invalid CPF is blocked on step 1 and explains itself; an e-mail and a mobile follow their own rules', async ({
    page,
  }) => {
    await openPix(page);
    await page.getByLabel('Pix key').fill('11111111111');
    await expect(page.getByText('Enter a valid CPF, for example 529.982.247-25.')).toBeVisible();
    await expect(next(page)).toBeDisabled();

    await page.getByRole('radio', { name: 'E-mail' }).click();
    await page.getByLabel('Pix key').fill('a..b@example.com');
    await expect(page.getByText('Enter a valid e-mail address.')).toBeVisible();
    await page.getByLabel('Pix key').fill('maria@example.com');
    await expect(next(page)).toBeEnabled();

    await page.getByRole('radio', { name: 'Mobile' }).click();
    await page.getByLabel('Pix key').fill('11912345678');
    await expect(page.getByLabel('Pix key')).toHaveValue('(11) 9 1234-5678');
    await expect(next(page)).toBeEnabled();
    expect(pixRows()).toBe(0);
  });

  test('an amount above the balance is blocked on step 2', async ({ page }) => {
    await openPix(page);
    await page.getByLabel('Pix key').fill(CPF);
    await next(page).click();
    await page.getByLabel('Pix amount').fill('5000.01');
    await expect(page.getByText("The amount can't exceed your available balance.")).toBeVisible();
    await expect(next(page)).toBeDisabled();
    await page.getByRole('button', { name: 'Use full balance' }).click();
    await expect(next(page)).toBeEnabled();
    expect(pixRows()).toBe(0);
  });

  test('a lost answer is uncertain; "Try again" reuses the key: ONE Pix, debited once', async ({
    page,
  }) => {
    const keys: (string | undefined)[] = [];
    let first = true;
    await page.route(ENDPOINT, async (route) => {
      keys.push(route.request().headers()['idempotency-key']);
      if (first) {
        first = false;
        await route.fetch(); // the server (and the real MaiaBank) process it...
        await route.abort('failed'); // ...but the answer never reaches the browser
      } else {
        await route.continue();
      }
    });
    await openPix(page);
    await toReview(page, '50');
    await page.getByRole('button', { name: 'Confirm and send' }).click();

    await expect(
      page.getByRole('heading', { level: 1, name: 'Outcome not confirmed' }),
    ).toBeVisible();
    await expect(page.locator('app-result-panel')).toContainText(
      "Check your transactions before trying again, so you don't pay twice.",
    );
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page);
    expect(balance()).toBe(4950);

    await page.getByRole('button', { name: 'Try again' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Pix sent' })).toBeVisible();
    expect(keys).toHaveLength(2);
    expect(keys[1]).toBe(keys[0]);
    expect(balance()).toBe(4950);
    expect(pixRows()).toBe(1);
  });

  test('a request that never leaves (network down) is uncertain and is not retried by itself', async ({
    page,
  }) => {
    let sent = 0;
    await page.route(ENDPOINT, async (route) => {
      sent++;
      await route.abort('internetdisconnected');
    });
    await openPix(page);
    await toReview(page, '50');
    await page.getByRole('button', { name: 'Confirm and send' }).click();

    await expect(
      page.getByRole('heading', { level: 1, name: 'Outcome not confirmed' }),
    ).toBeVisible();
    await page.waitForTimeout(1500);
    expect(sent).toBe(1);
    expect(balance()).toBe(5000);
    expect(pixRows()).toBe(0);
  });

  test('"Fail send Pix." (422 from the server) shows the not-sent message and keeps everything for another try', async ({
    page,
  }) => {
    await page.route(ENDPOINT, (route) =>
      route.fulfill({
        status: 422,
        contentType: 'application/json',
        body: JSON.stringify({ code: 'UNPROCESSABLE_ENTITY', message: 'Fail send Pix.' }),
      }),
    );
    await openPix(page);
    await toReview(page, '50');
    await page.getByRole('button', { name: 'Confirm and send' }).click();

    await expect(page.getByRole('heading', { level: 1, name: 'Pix not sent' })).toBeVisible();
    await expect(page.locator('app-result-panel')).toContainText(NOT_SENT);
    await expect(page.locator('app-result-panel')).toContainText('Fail send Pix.');
    await page.waitForTimeout(700);
    await expectNoSeriousA11yViolations(page);
    expect(balance()).toBe(5000);

    await page.getByRole('button', { name: 'Edit and try again' }).click();
    await expect(page.getByRole('button', { name: 'Confirm and send' })).toBeVisible();
  });

  test('the exact full balance can be sent and leaves the balance at zero', async ({ page }) => {
    await openPix(page);
    await toReview(page, '5000.00');
    await page.getByRole('button', { name: 'Confirm and send' }).click();
    await expect(page.getByRole('heading', { level: 1, name: 'Pix sent' })).toBeVisible();
    expect(balance()).toBe(0); // the exact balance is allowed
    expect(pixStatus()).toBe('COMPLETED');
  });
});

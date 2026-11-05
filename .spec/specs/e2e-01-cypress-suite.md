# SPEC-E2E-01 — Cypress end-to-end suite (one spec for the whole product)

| Field | Value |
|---|---|
| Status | Draft (written 2026-11-04; not authorized to implement) |
| Track · Wave | Frontend + Backend black-box · after the backend close-out (BE-07 + BE-08) |
| Depends on | BE-07, BE-08 (merged), SL-00…SL-06 (merged) |
| May run in parallel with | none |
| Findings / decisions | D-31 (slices), D-58, owner decision 2026-11-04: **Cypress is added next to Playwright; nothing is removed in this spec** |
| Authorization | Written by the agent at the owner's request. Approval of this document is **not** approval to implement (`shared/como-executar.md`). |

## 1. Objective
One Cypress suite that proves every user-visible flow of Maia Bank end to end on the **real chain** (Angular app → mock identity provider → Spring API → PostgreSQL → Kafka → the real `maiabank-api` container) on three viewports and both themes, plus a small black-box API matrix. It replaces nothing: the 114 Playwright tests stay green and untouched. The suite is the safety net the owner relies on before and after the **manual test round** that closes the project (§10), and the place where the improvements found in that round get a regression test.

## 2. Context to read first
`.spec/memory/produto.md`, `.spec/memory/contexto-tecnico.md` (D-54 idempotency, D-55 body shape, D-56 operation shell, D-57/D-58 recharge and Pix), `app-financial-transaction-service/e2e/README.md` (mock IdP, users, stack), the Playwright files `e2e/*.e2e.ts` (the scenarios to mirror), `.spec/specs/final-01-test-round.md` (items C, E, F, H), `docs/prototype-deviations.md`.

## 3. Scope
**In scope**
* A Cypress project in `app-financial-transaction-service/cypress/` (TypeScript, Cypress 15.x pinned, `cypress-axe` for accessibility), run with `npm run cy:run` (headless) and `npm run cy:open`.
* Specs (one file per area) with the scenario ids of §5; viewports **1280×800, 768×1024, 390×844** and themes **dark and light**.
* A black-box API matrix (`cy.request`) for the endpoints in `produto.md`, including 401/403/404/422, idempotent replay, pagination, correlation id and the management port.
* A GitHub Actions workflow `e2e.yml` that starts the compose stack (`docker-compose.yml` with `--env-file .env --env-file e2e.env --profile e2e`), waits for the health check and runs the suite.
* The manual-round checklist (§10) and a place to log findings: `app-financial-transaction-service/docs/manual-round.md`.

**Out of scope:** removing or porting away Playwright, Cypress Cloud / dashboards, visual-regression screenshots, performance tests, real Auth0 (never automated; the owner signs in by hand).

## 4. Constraints and how Cypress must be made to fit
These are the technical traps of this app; the executor must solve them, not work around the product:
1. **Cross-origin sign-in.** The app redirects to the mock IdP (`http://mock-idp:9099`). Use `cy.origin('http://mock-idp:9099', …)` for the "choose a user" button, and map the host with `--host-resolver-rules="MAP mock-idp 127.0.0.1"` in `on('before:browser:launch')` (Chromium family). Set `chromeWebSecurity` only if `cy.origin` cannot do the job; record why.
2. **Tokens live in memory.** A full page load signs the customer out, so after sign-in navigation is **client-side** (`history.pushState` + `popstate`, exactly as the Playwright helper `goInApp`). Provide `cy.goInApp(url)`.
3. **`/config.json`** is replaced per test (`cy.intercept('/config.json', …)`) so the app talks to the mock IdP, never to Auth0 (copy of `useMockIdp`).
4. **Database assertions** (balance, ledger rows, `idempotency_key`, `outbox_event`) go through `cy.task('sql', statement)` implemented in `setupNodeEvents` with `docker exec pg-db psql …` (same commands as Playwright). A `beforeEach` resets the demo customer (account 2022: balance 5,000.00, empty ledger, no idempotency keys).
5. **Lost answer** (the server processed the request, the browser never saw the answer): `cy.intercept(…, (req) => req.continue((res) => res.destroy()))` for the first call only, then pass-through. **Network down:** `forceNetworkError: true`.
6. **No automatic POST retry** is asserted by counting intercepted requests (`cy.get('@send.all')`).
7. **Accessibility:** `cy.injectAxe()` + `cy.checkA11y(null, { includedImpacts: ['serious', 'critical'] })` after each screen has finished its entrance animation (wait for the element, then `cy.wait(700)` only where an animation is running; prefer `should` assertions elsewhere).
8. **Secrets:** no password is ever typed or stored; the mock IdP is the only login. The suite fails if a JWT-shaped string appears in a visited URL or in the console (`watchLeaks` equivalent).
9. Everything runs on **one worker, in order** (shared database); no `cy.wait(ms)` for synchronisation except the animation case above.

## 5. Scenarios (ids are stable; every one runs on the three viewports unless marked)
**Access (`auth.cy.ts`)**
* A-01 landing → sign in as Maria → Home greets "Hello, Maria!" with account 2022 and R$ 5,000.00. A-02 João sees account 3033 / R$ 1,000.00. A-03 the unlinked customer gets "No access". A-04 expired tokens → one renewal → "Session expired" dialog. A-05 sign out returns to the landing page and the session is gone. A-06 an unauthenticated visit to `/home` goes to sign-in. A-07 no token in URLs or console.

**Home and statement (`home-statement.cy.ts`)**
* H-01 totals on Home equal the statement totals for the same period; the chart has its table fallback. H-02 period chips 7/30/90. H-03 statement filters (type), sort, pagination "Showing x–y of n", single-day range. H-04 transaction detail and "Copy transaction id". H-05 **user 2 opening user 1's transaction gets "Transaction not found", never its data.** H-06 empty state for a customer without activity. H-07 Home quick actions: Pix and Deposit tiles, quick-recharge card.

**Withdraw (`withdraw.cy.ts`)**
* W-01 withdraw 100 → one POST `{amount}`, balance 4,900, one `WITHDRAW` row, receipt; double click = one request. W-02 the exact full balance leaves 0. W-03 above the balance → server 422 panel, balance unchanged, "Edit and try again" keeps the value. W-04 lost answer → "Outcome not confirmed" → **Try again** sends the same `Idempotency-Key`, debited once. W-05 network down → one request, no retry.

**Deposit (`deposit.cy.ts`)**
* D-01 generate the demo boleto (every surface says demo; actions only toast "Demo action") → **Simulate payment** → balance +250 once, one `DEPOSIT` row, body `{agency, account, amount}` of the caller. D-02 zero is blocked with "Enter an amount greater than zero." D-03 "New amount" deposits nothing. D-04 lost answer replay credits once. D-05 forced 404 → field error "Account not found.". D-06 network down → uncertain, no retry.

**Recharge (`recharge.cy.ts`)**
* R-01 Vivo 30 → body `{areaCode:11, phoneNumber:912345678, carrier:"VIVO", amount:30}`, balance −30, `PHONE_RECHARGE` row, **exactly one new outbox event**, success text "Recharge requested…" and the page never contains "completed". R-02 invalid number or amount keeps Review disabled and explains. R-03 above the balance → business panel, nothing published. R-04 lost answer replay debits once (one event). R-05 network down. R-06 Home quick-recharge card hands over to the Recharge page and opens the same review (nothing sent from Home, no phone in the URL).

**Pix (`pix.cy.ts`)**
* P-01 CPF `529.982.247-25`, 50 → three steps, **masked** key in the review and receipt (the full key is nowhere in the DOM), one POST with the plain key, balance 4,950, one `COMPLETED` `PIX` row against the real `maiabank-api`. P-02 CPF/e-mail/mobile rule cases shared with the backend (`11111111111`, `52998224724`, `a..b@example.com`, `(11) 8 …`) are blocked on step 1. P-03 above the balance is blocked on step 2; "Use full balance" works and the exact balance can be sent. P-04 lost answer → Try again → one Pix, debited once. P-05 forced 422 "Fail send Pix." → "We couldn't send this Pix. Your balance was not changed." P-06 network down. P-07 **MaiaBank stopped** (`docker stop maiabank-api`, restored in `after`): the Pix is refused, the balance and the ledger are consistent (`FAILED` row, balance unchanged); runs last and waits for the circuit breaker to close before finishing.

**Cross-cutting (`cross.cy.ts`)**
* X-01 axe (serious/critical) on landing, sign-in, Home, Statement, Detail, each money form, review/dialog and every result panel — dark **and** light. X-02 keyboard-only completion of each money flow (Tab/Shift+Tab/Enter/Space/Escape; focus trap and focus return of the dialogs). X-03 theme toggle persists across client-side navigation. X-04 `prefers-reduced-motion` removes the entrance animations. X-05 refresh mid-flow returns to sign-in without leaking data. X-06 the 404 page and a wrong URL.

**API black box (`api.cy.ts`, one viewport, `cy.request` with tokens obtained from the mock IdP)**
* B-01 401 without a token, 401 for another audience, 403 for an unlinked subject, 404 for another customer's id. B-02 body-shape matrix on the four POSTs (null, `{}`, blank, zero, negative, **three decimals → 422 "Amount must have at most 2 decimal places"**) never 500. B-03 `Idempotency-Key`: replay, different body (422), over 64 characters (422), parallel same key (one effect). B-04 statement pagination, single-day range, bad dates (400/422 as documented). B-05 `X-Request-Id` echoed and generated; unsafe values replaced. B-06 management port: `/actuator/health` UP and `/actuator/prometheus` answer on **8090** (from inside the compose network via `docker exec`), the actuator is absent on 8088. B-07 no personal data in the API container log after the whole run (`docker logs` scanned for the demo account number, CPFs, phones, tokens).

## 6. Tasks
- [ ] HUMAN: approve this spec (and confirm Cypress 15.x and `cypress-axe` as new dev dependencies)
- [ ] Install Cypress + `cypress-axe`, `cypress.config.ts`, `tsconfig`, npm scripts `cy:open` / `cy:run`; `.gitignore` for videos and screenshots
- [ ] Support layer: `cy.signInAs`, `cy.goInApp`, `cy.useMockIdp`, `cy.task('sql')`, `cy.resetDemoCustomer`, `cy.a11y`, request counters, leak watcher
- [ ] `auth.cy.ts` (A-01…A-07)
- [ ] `home-statement.cy.ts` (H-01…H-07)
- [ ] `withdraw.cy.ts`, `deposit.cy.ts` (W-*, D-*)
- [ ] `recharge.cy.ts`, `pix.cy.ts` (R-*, P-*, including the MaiaBank-stopped case)
- [ ] `cross.cy.ts` (X-01…X-06) in both themes
- [ ] `api.cy.ts` (B-01…B-07)
- [ ] Workflow `.github/workflows/e2e.yml` (compose up with health wait, Node, `npm ci`, `npm run cy:run`, upload screenshots/videos on failure)
- [ ] `e2e/README.md` / `cypress/README.md`: how to run both suites; `docs/manual-round.md` created from §10
- [ ] Run the whole suite three times in a row (flake check) and fix or quarantine with a written reason

## 7. Acceptance criteria
* [ ] Every scenario id of §5 exists and passes on 1280, 768 and 390 (the API matrix once).
* [ ] The Playwright suite still passes untouched (114 tests).
* [ ] No `cy.wait(<ms>)` outside the documented animation case; no `force: true` clicks (a needed force is a product bug to report).
* [ ] Three consecutive full runs are green on the same stack with no retry configured.
* [ ] Failures leave a screenshot and the request log; the console and URLs contain no token.
* [ ] `e2e.yml` is green on a pull request.

## 8. Mandatory verification
`docker compose --env-file .env --env-file e2e.env --profile e2e up -d --build` → wait for `healthy` → `cd app-financial-transaction-service && npm run cy:run` (3×) and `npm run e2e` (Playwright, once). Report what was **executed** and what was only written.

## 9. Approval gates / Stop and ask
Stop and ask before: adding any dependency other than Cypress and `cypress-axe`; changing product code to make a test pass (report it as a defect instead, with a regression test); weakening an assertion; removing a Playwright test; touching the mock IdP behaviour used by Playwright.

## 10. Manual round after the suite (owner) — checklist and where to log findings
Do this on the real Auth0 login (`npm start` + API from the IDE or `docker compose up -d --build`), in a normal browser, then write each finding in `docs/manual-round.md` as **ID · screen · what happened · expected · severity (blocker / should / nice) · screenshot**. Improvements become new specs or small fixes; each one gets a Cypress scenario.
1. **Sign-in and session:** both customers; sign out; refresh on each page; expired session; two tabs.
2. **Home:** balance count-up, period chips, chart and its table, recent rows open the detail, Quick actions, quick recharge hand-over.
3. **Money flows, each one:** happy path; double click; browser Back during the review; typing odd values (`1,5`, `0,00`, `10.005`, huge numbers, pasted text); airplane mode mid-request and **Try again**; slow network (DevTools throttling).
4. **Pix:** each key type with good and bad keys, the masked review, a value above the balance, MaiaBank stopped (`docker stop maiabank-api`).
5. **Recharge:** each carrier; check the event in Kafdrop (`http://localhost:19000`).
6. **Statement:** filters, sort, pagination, a single day, detail and copy id.
7. **Look and feel:** dark and light, 1280 / 768 / 390 / **320 px**, keyboard only, screen reader spot check, zoom 200 %, reduced motion.
8. **Copy and wording:** every message in English, no "completed" for a recharge, demo labels on the boleto, no real data claims.
9. **Operations:** the API log has no personal data; `/actuator` only on 8090; the request id shown in an error matches the API log.
10. **Anything that felt slow, confusing or ugly** — the most valuable findings.

## 11. Skills / tools
`angular-developer` (Angular testing conventions), `ui-ux-pro-max` (only to judge findings), Cypress docs for `cy.origin`, `cy.intercept` and `res.destroy()`.

## 12. Result summary (filled on completion)
_Result, checks run (executed vs written), flake report, accepted limitations._

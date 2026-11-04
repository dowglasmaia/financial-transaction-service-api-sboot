# SPEC-SL-04 — Deposit and mobile recharge (one slice, two pages)

**Status:** Implemented on `slice/04-deposit-and-recharge`, awaiting the owner's PR, review and green pipelines (merged with the former SL-05 on 2026-11-04) · **Depends on:** SL-03 · **Parallel with:** none
**Branch:** `slice/04-deposit-and-recharge` · **Track:** Backend (tests and small fixes) + Frontend + E2E
**Absorbs:** the former **SL-05** (recharge), FE-04 (Deposit, Recharge), the operation-specific parts of BE-05. The body *shape* validation of both operations was already done in SL-03.
**Read first:** `../memory/produto.md` (rules 4, 6, 7: carriers, outbox and the byte-identical Kafka payload; third-party deposits allowed, agency ignored), `../memory/contexto-tecnico.md` (*prototype vs backend* case, BE-03 outbox, ARCH-02 code map), `../spec-execution-flow.md` §1b, v2 reference (Deposit + boleto card, Recharge page, Home quick-recharge card). Protocol: `../shared/como-executar.md`.

## Why one slice
After SL-03 both operations are mostly frontend: the same amount presets, review dialog, result panel, operation shell and idempotency are reused, and the backend only needs operation-specific tests. Two pages in one PR is smaller than SL-02 and saves a full slice cycle. **Order inside the slice: Deposit first (own commits), then Recharge.**

## Objective
1. **Deposit** with the v2 experience (enter an amount → **generate a boleto** → **simulate payment**) while the money movement is the real `POST /transactions/deposit`; resolves the Tier-2 divergence recorded since FE-01 (proposal A in `docs/prototype-deviations.md`).
2. **Recharge** a mobile number: the debit and the ledger row are immediate, the carrier credit is asynchronous (Kafka event through the transactional outbox), so the UI promises only that the recharge was **requested**.

## Human action before
Approve the deposit reconciliation (default): the boleto screen is a **client-side simulation clearly labelled as demo**; **"Simulate payment"** sends `POST /transactions/deposit {agency, account, amount}` for the **caller's own account** (agency and number from `/accounts/me`); no boleto is stored or fetched. If the owner prefers a different behaviour, the deposit part stops. Recharge needs nothing (carriers are the backend enum `VIVO | CLARO | TIM | ALGAR`; v2's "Oi" becomes Algar, deviation V3).

## Scope
**Backend** (the use cases exist since ARCH-02; this is tests plus small fixes)
- Deposit: endpoint and semantics unchanged (rule 7); unknown account → 404 "Account not found." (kept); third-party deposit still works and the receiver's statement shows it; idempotency from SL-03 applies.
- Recharge: the phone rule stays (`^[1-9]{2}9[0-9]{8}$` over area code + number, `MobileRecharge.hasValidPhone`); unknown carrier and non-positive amount → 422 with the existing messages; **Kafka topic and payload stay byte-identical** (`OutboxRechargeEventPublisherTest` and `PixRechargeCharacterizationTest` must stay green); debit + ledger + outbox in one transaction; idempotency applies.
- Contract: examples/enums only if the preview shows drift (carrier enum documented as the four values).

**Frontend** (reuses the SL-03 pieces: amount presets, review dialog, result panel, operation shell, `Idempotency-Key`)
- **Deposit page:** amount with presets (R$ 100/250/500/1000), validation "Enter an amount greater than zero."; **"Generate boleto"** shows a preview panel, then a boleto card (beneficiary "Maia Bank (demo)", payer from the ID token, document id, due date, amount, decorative barcode and digitable line) with **Download boleto** / **Share** / **Copy line** (toast "Demo action", no real file or link), **Simulate payment** and **New amount**; footer "Demo. This is not a real boleto." "Simulate payment" runs the shared shell: success "Deposit registered" (balance and a `DEPOSIT` row refresh), 404 → field-level error, 422 → backend message, uncertain outcome as in SL-03.
- **Recharge page:** carrier cards (Claro, Vivo, TIM, Algar; brand-coloured wordmarks as text, no logos), masked phone input `(11) 9 1234-5678` that splits into `areaCode` + `phoneNumber`, value presets (R$ 15/30/50/100) + custom amount, the SL-03 balance feedback, review dialog (carrier / number / amount), result variants. Success copy: **"Recharge requested. Your carrier will apply the credit shortly."**, never "completed".
- **Home:** the "Deposit" tile and the quick-recharge card (same form component, same review dialog); nav entries "Deposit" and "Recharge" active.

**Out of scope:** depositing into another customer's account from the UI (the API still allows it), real boleto issuing/reconciliation, recharge history, carrier lookup, more carriers.

## Tasks
- [x] HUMAN: approve the boleto-simulation reconciliation
> ✅ 2026-11-04 — approved by the owner through the execution prompt (no server-side boleto, no new endpoint, demo labels, third-party deposits not in the UI). Verification: owner's instruction.
- [x] BE tests first (red) — deposit: unknown account 404, third-party deposit visible in the receiver's statement, replay with the same key credits once
> ✅ 2026-11-04 — `DepositRechargeOperationsTest` (deposit part: own row in the statement, third-party credit visible only to the receiver, replay credits once, 404 frees the key). Verification: `./mvnw -B -Dtest=DepositRechargeOperationsTest test` → red first only because of my test setup (statement needs date parameters; an overflowing phone has another 422 message), then 23/23 green: the backend already behaved correctly.
- [x] BE tests first (red) — recharge: invalid phone / unknown carrier / non-positive amount → 422 (no NPE); payload bytes equal the characterization; debit + ledger + outbox in one transaction; replay with the same key debits once
> ✅ 2026-11-04 — same class (recharge part): 6 invalid phones, 5 unknown carriers, 5 bad amounts → 422 and nothing moves; replay debits once with one ledger row and one outbox event; the four contract carriers accepted. Payload bytes stay covered by `PixRechargeCharacterizationTest`/`OutboxRechargeEventPublisherTest` (green); failure injection by `FailureInjectionTest` (green). Verification: `./mvnw -B verify` (JDK 21) → 285 tests, 0 failures, 1 skipped.
- [x] BE: fixes only where a red test demands; `./mvnw -B verify` green; `check-architecture.mjs --strict` 0 violations
> ✅ 2026-11-04 — no production fix was needed. Verification: `./mvnw -B verify` → BUILD SUCCESS; `check-architecture.mjs --src src/main/java --base-package com.dowglasmaia.financialtransaction --strict` → 84 files, 0 violations, 1 warning (W04, as before); `ArchitectureTest` 15/15.
- [x] FE tests first (red) — deposit: amount validation; boleto deterministic from the amount; "Simulate payment" sends one request with the caller's agency/account/amount; double click → one request; no retry
> ✅ 2026-11-04 — api, deterministic boleto, page flow (one request on double click, 404 field error, 422 panel, uncertain + same key). Red: modules did not resolve (commit 5131095); green with the implementation. Verification: `npm test` → 277 passed.
- [x] FE: Deposit page, boleto card, result variants (own commits)
> ✅ 2026-11-04 — Deposit page, demo boleto, nav entry. Verification: `npm run build` no warnings; e2e below.
- [x] FE tests first (red) — recharge: phone mask and area-code/number split; carrier selection; copy says "requested"; one request on double click; no retry
> ✅ 2026-11-04 — phone mask/split, api, page (carrier, split, 'requested' never 'completed', one request, same key on Try again, prefill from Home), Home tile and card. Red: modules did not resolve (commit e45c8cf). Verification: `npm test` → 307 passed.
- [x] FE: Recharge page, carrier cards, Home quick-recharge card, result variants (own commits)
> ✅ 2026-11-04 — Recharge page, shared recharge form, Home quick-recharge card. Verification: `npm run build` (739 kB initial, under the 800 kB warning, no warnings).
- [x] E2E: deposit generate → simulate → balance up by exactly the amount and a `DEPOSIT` row; recharge 30 → balance −30, `PHONE_RECHARGE` row, copy contains "requested" and not "completed"; invalid amount/phone blocked; forced network failure → uncertain panel; axe on both pages
> ✅ 2026-11-04 — `deposit.e2e.ts` (6) and `recharge.e2e.ts` (6 incl. Home) × 3 viewports, axe no serious violations on forms, dialogs, boleto and panels. Verification: full `npx playwright test` → 91/93; the 2 failures were an old statement selector `getByRole('link', {name:/Deposit/})` that now also matched the new nav entry (fixed with `locator('main')`); rerun of statement+deposit+recharge → 51 passed.
- [x] Manual: the event appears on `recharge-mobile-topic` in Kafdrop (`http://localhost:19000`); record what was seen
> ✅ 2026-11-04 — Kafdrop `http://localhost:19000/topic/recharge-mobile-topic/messages` (built-in browser): offsets 0–19; the UI recharges appear as `{"phoneNumber":912345678,"areaCode":11,"carrier":"VIVO","amount":30}` and `…"carrier":"ALGAR","amount":15}`, key empty, header `__TypeId__: com.dowglasmaia.provider.model.MobileRechargeRequestDTO`. The ALGAR recharge whose answer was lost and replayed with the same key produced ONE event.
- [x] Postman: deposit (incl. third-party) and recharge requests
> ⚠️ 2026-11-04 — folder "SL-04 deposit and recharge" (9 requests: own and third-party deposit, 404, deposit/recharge with a key, invalid phone/carrier/amount) written, NOT executed (set `otherAccount` first). The same cases are proven by the backend tests and e2e.
- [x] Docs: close proposal A and update the carrier row in `docs/prototype-deviations.md`
> ✅ 2026-11-04 — `docs/prototype-deviations.md` rows 31–39, proposal A and V3 closed. Verification: diff reviewed.

## Acceptance
- [x] Deposit: the balance increases by exactly the amount, once, and the statement shows a `DEPOSIT` row; the UI never claims a real boleto exists (demo wording on every boleto surface)
> ✅ e2e: balance 5000 → 5250 once (psql), one DEPOSIT row, statement shows it; demo wording on the preview, card, badge and footer.
- [x] Recharge: success text says "requested", never "completed"; Kafka payload and topic identical to the characterization; balance, ledger and outbox stay consistent when the request fails halfway (the failure-injection tests stay green)
> ✅ e2e: 'requested', body has no 'completed'; payload and topic seen in Kafdrop; failure-injection tests green.
- [x] No auto-retry; double click sends one request (both pages)
> ✅ unit + e2e: double click = 1 POST on both pages; network down = 1 POST, no retry.

## Minimal validation (required to close this spec)
- [x] `./mvnw -B verify` green; `npm ci && npm run build && npm test` green; E2E green on 3 viewports (1280/768/390); axe no serious violations
> ⚠️ `./mvnw -B verify` ✅ (285), `npm run build` ✅, `npm test` ✅ (307), e2e ✅ on 1280/768/390, axe ✅. `npm ci` NOT run (dependencies unchanged).
- [x] Browser check of both full flows + Kafdrop observation
> ✅ screenshots at 1280 and 390 reviewed (Home, boleto, recharge form) with the mock IdP; Kafdrop observed. The owner still does the real Auth0 run.
- [ ] Both pipelines green on the PR
> ⏳ not executed: they run on the PR the owner opens.

## Stop and ask
- Any server-side boleto, new endpoint or table; changing who can be credited; any change to the Kafka topic, key, headers or payload; adding carriers

## Skills
java-modern-engineering, java-spring-create-module (only if a use case changes), angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max

## Result
**Result (2026-11-04).** Deposit and Recharge pages, on top of the SL-03 pieces, with no backend production change.
* **Deposit:** demo boleto built on the device; "Simulate payment" is the real `POST /transactions/deposit` for the caller's own account, through the shared operation shell and `Idempotency-Key`. 404 is a field error; 422 and uncertain use the shared panels.
* **Recharge:** carrier cards, masked number split into `areaCode` + `phoneNumber`, amount presets, review dialog; success says "requested". Kafka topic, key, headers and payload are unchanged.
* **Home:** Deposit tile and the quick-recharge card (same form; it opens the same review on the Recharge page).
* **Shared:** `shared/ui/recharge-form`, `shared/util/phone`, `shared/util/carriers`, `AmountPresets.serverError`.
* **Checks run:** backend 285 tests (JDK 21), architecture strict 0 violations, 307 unit tests, build, e2e 93 (3 viewports) with axe, Kafdrop observation. **Only written / not run:** the 9 Postman requests, `npm ci`, both pipelines (on the PR), the real Auth0 login (owner).
* **Accepted limits:** the demo "Simulate payment" has no review dialog; Review recharge stays enabled above the balance (feedback only, the server answers 422); the Home card does not send anything itself; the carrier credit is asynchronous so the UI cannot confirm it. The local JDK of the shell was 25: the build must run with JDK 21.

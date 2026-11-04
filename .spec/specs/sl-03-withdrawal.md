# SPEC-SL-03 — Withdrawal + shared result panel + idempotency

**Status:** Implemented on `slice/03-withdrawal`, awaiting owner review and green pipelines (executed 2026-11-04) · **Depends on:** SL-02 · **Parallel with:** none
**Branch:** `slice/03-withdrawal` · **Track:** Backend + Frontend + E2E
**Absorbs:** FE-04 (Withdrawal, shared result panel), BE-06 (`Idempotency-Key`, Flyway `V4`), BE-05 (null/invalid body handling for **all four POST operations**, decided 2026-11-04).
**Read first:** `../memory/produto.md` (rules 1, 2; ATM withdrawal), `../memory/contexto-tecnico.md` (BE-03 design), `../spec-execution-flow.md` §1b, v2 reference (Withdraw, review dialog, result panel in its three outcomes). Protocol: `../shared/como-executar.md`.

## Objective
The simplest money flow, built first because it also creates what the other three flows reuse: the **review → confirm → result** pattern with a success / business-failure / **uncertain-outcome** panel on the frontend, and the **`Idempotency-Key`** protection on the backend so a retried or double-submitted write can never debit twice.

## Human action before
Approve the idempotency design (defaults below): header optional; same key + same body → replay of the original result; same key + different body → 422 "Idempotency-Key reuse with a different request"; keys scoped per customer; TTL 24 h.

## Scope
**Backend**
- `Idempotency-Key` (optional header, max 64 chars) on **all four POST money endpoints** via one component (Flyway **`V4`**: table keyed by `(subject, key)` with request hash, stored status, `created_at`); the replay returns the stored status/body without touching balance, ledger or outbox. Concurrent identical keys: one wins, the other waits or replays — no double debit. **Placement (skill D8):** a generic component in `shared.idempotency` (servlet filter around the four POST paths plus a small store on the `V4` table; the key scope is the token `sub`); it imports no feature and replays without calling the use cases.
- **Body shape validation of all four POST operations, once** (withdrawal, deposit, recharge, Pix): null body, missing, blank or non-positive fields → 422 with the existing messages, never 500/NPE (the checks live in `ValidateBodyRequest`, `transaction.api`). The Pix key rules (CPF check digits, e-mail, phone; F-11) stay in SL-06 and the recharge phone/carrier rules in SL-04; those slices only add their operation-specific tests.
- Withdrawal: `{amount}` only is the supported body; null body / missing or non-positive amount → 422 with the existing messages (never 500); optional `agency`/`numberAccount` keep their BE-04 semantics (owner's own number else 404).
- Contract: header parameter and replay behaviour documented (preview → approval).

**Frontend**
- Shared **amount presets** (`app-amount-presets`: preset buttons, "Use full balance", the balance feedback) and **review dialog** (`app-review-dialog`: title, receipt lines, confirm/cancel, pending state), reused by SL-04 and SL-06 and documented in `shared/ui`.
- Shared **result panel** (`success` / `business failure` / `uncertain`) with icon animation, receipt lines, and actions (New …, Back to home, View transactions, Try again); **operation shell** that guarantees: one in-flight request, button disabled while pending, double-click sends exactly one request, **no automatic retry of any POST**.
- `Idempotency-Key`: generated per user attempt; reused only when the user explicitly presses "Try again" after an uncertain outcome.
- **Withdraw page:** amount with presets (R$ 50/100/200/500) and "Use full balance"; client feedback "The amount can't exceed your available balance." and "Enter an amount greater than zero." (feedback only; the server decides); review dialog; request body is exactly `{amount}`; result variants:
  * success → "Withdrawal registered" (never claims cash was dispensed);
  * business failure (422) → backend message surfaced, balance unchanged;
  * uncertain (network error/timeout/5xx without a response) → "We couldn't confirm the operation. Check your transactions before trying again, so you don't pay twice." with a link to Transactions.
- After success: balance (Home, rail pill) and statement refresh from the API; a new `WITHDRAW` row appears.
- Nav entry "Withdraw" active.

**Out of scope:** non-card UI for another account number, ATM card flows, deposit/recharge/Pix screens.

## Tasks
- [x] HUMAN: approve the idempotency design
> ✅ 2026-11-04 — approved by the owner with the SL-03 prompt (header optional, replay, 422 on reuse, per-customer scope, 24 h).
- [x] CONTRACT: preview header + behaviour; wait for approval
> ✅ 2026-11-04 — preview shown, owner answered "Aprovado". Contract 1.2.0: component parameter `Idempotency-Key` on the four POSTs and response header `Idempotent-Replayed`. Verification: `npm run api:types` regenerated `schema.d.ts`.
- [x] BE tests first (red): null/blank/non-positive bodies of deposit, recharge and Pix → 422 without 500 (parametrized over the four operations); replay with the same key does not double-debit; same key different body → 422; parallel same-key requests debit once; no key behaves as today; null/zero/negative amount → 422 without 500; exact-balance withdrawal → 204
> ✅ 2026-11-04 — commit 09c5081. Red run: `BodyShapeValidationTest` 11 of 31 failed (400/404/204 instead of 422, one wrong message), `IdempotencyTest` 11 of 11 errored (no table `idempotency_key`), `ValidatorsCharacterizationTest` 1 failed. Verification: `./mvnw -Dtest=BodyShapeValidationTest,IdempotencyTest,ValidatorsCharacterizationTest test` → 50 run, 12 failures, 11 errors. Exact-balance 204 and above-balance 422 were already covered by `DepositWithdrawCharacterizationTest` (green before and after).
- [x] BE: `V4` + idempotency component wired to all four POSTs; suite green
> ✅ 2026-11-04 — commit 33a7c86. Flyway `V4__idempotency_key.sql`, `shared.idempotency` (`IdempotencyFilter`, `IdempotencyStore`, `CachedBodyRequest`), `ValidateBodyRequest` hardened for all four bodies, `GlobalExceptionHandler` answers 422 for a missing or unreadable body. Verification: `./mvnw -B verify` (JDK 21) → 262 tests, 0 failures, 1 skipped (live tenant); `check-architecture.mjs --strict` → 0 violations, 1 warning (W04, 16 classes in `transaction.domain`, pre-existing).
- [x] FE tests first (red): double click → one request; no retry on failure; body is `{amount}` only; key reused only on explicit retry; three result variants render; formatting of the amount
> ✅ 2026-11-04 — commit b1775e0. Red run: `npm test` could not resolve `./operation`, `./withdraw.api`, `./withdraw-page` nor `amountFeedback`. Verification: `npm test` → compile errors before the implementation.
- [x] FE: result panel + operation shell + amount presets + review dialog + Withdraw page
> ✅ 2026-11-04 — commit 0fbee81. `OperationController`, `app-amount-presets`, `app-review-dialog`, `app-result-panel`, `WithdrawPage`, route and nav entry "Withdraw". Verification: `npm test` → 33 files, 260 tests passed; `npm run build` → no errors or warnings.
- [x] E2E: withdraw 100 → balance down by 100 and a row appears; exact full balance → balance 0; amount above balance → business-failure panel with unchanged balance; forced network failure → uncertain panel and "Try again" reuses the key (backend debits once); axe on the page and the dialog
> ✅ 2026-11-04 — `e2e/withdraw.e2e.ts`, 5 scenarios on 3 viewports. Verification: `npx playwright test e2e/withdraw.e2e.ts` → 15 passed; full suite `npx playwright test` → 57 passed (42 earlier + 15). Covers: double click = one POST with body `{"amount":100}`, balance 5000 → 4900 and one WITHDRAW row; full balance → 0; 5000.01 → 422 panel, balance 5000; answer lost after the server processed it → uncertain panel, Try again sends the same key, balance 4900 and one row; network down → one request, no automatic retry; axe (page, dialog, result panels) no serious violations.
- [x] Postman: withdrawal requests with and without `Idempotency-Key`
> ⚠️ 2026-11-04 — written, NOT executed: folder "SL-03 withdrawal and Idempotency-Key" (8 requests). The same checks are proved by `IdempotencyTest` and the e2e.
- [x] Docs: result-panel usage in `features/…/doc.md`
> ✅ 2026-11-04 — `features/withdraw/doc.md` (shell, presets, review dialog and result panel usage) and `docs/prototype-deviations.md` rows 16–22.

## Acceptance
- [x] No money POST answers 500 for a null/blank/non-positive body (all four operations)
> ✅ `BodyShapeValidationTest`: 31 cases over the four operations (null, `{}`, blank, zero, negative, missing, malformed) → all 422 with the known message, balance and ledger untouched.
- [x] Replaying a POST with the same `Idempotency-Key` does not double-debit (proved for withdrawal; the component is reused by SL-04…06)
> ✅ `IdempotencyTest` (11 tests): replay debits once with `Idempotent-Replayed: true`; 12 parallel same-key requests → one debit, all 204; reuse with another body → 422; key scoped per customer; a failed attempt does not burn the key; 24 h expiry; the same component replays deposit, recharge (one outbox row) and Pix (one bank call).
- [x] Withdrawal request body is `{amount}` only; double click sends exactly one request; no POST is auto-retried
> ✅ unit (`withdraw.api.spec`, `withdraw-page.spec`, `operation.spec`) and e2e (the captured POST body is `{"amount":100}`; one request after a double click; one request after a network failure).
- [x] A debit of exactly the full balance succeeds; amount > balance → 422 and balance unchanged
> ✅ backend `DepositWithdrawCharacterizationTest` (exact balance 204; 5000.01 → 422, unchanged) and e2e (full balance → balance 0; 5000.01 → business panel, balance 5000).

## Minimal validation (required to close this spec)
- [x] `./mvnw -B verify` green (state the replay and concurrency tests)
> ✅ 262 tests, 0 failures, 1 skipped (JDK 21). Replay and concurrency: `IdempotencyTest`; money concurrency unchanged (`MoneyConcurrencyTest`).
- [x] `npm ci && npm run build && npm test` green; E2E green on 3 viewports; axe no serious violations
> ✅ `npm run build` clean, `npm test` 260 passed, e2e 57 passed on 3 viewports, axe no serious violations. (`npm ci` not re-run: dependencies unchanged.)
- [x] Browser check: withdraw, see balance and statement row change
> ✅ 2026-11-04 — Playwright screenshots at 1280 and 390 (form with presets, review dialog, uncertain panel) and the e2e reads the balance and the new row on screen and in PostgreSQL. A manual walk with the real Auth0 login stays with the owner.
- [ ] Both pipelines green on the PR
> ⏳ not executed: the owner opens the PR; `api-ci` (with `openapi-diff`, an additive change) and `app-ci` run there.

## Stop and ask
- New table beyond `V4`; 409 vs 422 on key reuse; any change to the success/204 contract

## Skills
java-persistence-performance (transactions-concurrency), java-modern-engineering, openapi-rest-contract, angular-module-scaffold, angular-tailwind-ui-specialist

## Result
**Result (2026-11-04, branch `slice/03-withdrawal`).**
* Backend: contract 1.2.0; Flyway `V4` (`idempotency_key`); `shared.idempotency` (servlet filter after Spring Security, key scoped to the token `sub`, SHA-256 of the body, claim with `insert ... on conflict do nothing`, replay of the stored status, wait for a running first attempt, takeover of a running row after 60 s, expiry 24 h, hourly purge); a 4xx/5xx frees the key; the body shape of the four POSTs is validated once (`ValidateBodyRequest`) and a missing, null or unreadable body is a 422.
* Frontend: `OperationController` (one in flight, no automatic retry, a key per attempt, the same key only on "Try again", 30 s timeout → uncertain), `app-amount-presets`, `app-review-dialog`, `app-result-panel`, Withdraw page and nav entry.
* Executed: backend `./mvnw -B verify` (262 tests, 0 failures, 1 skipped), strict architecture checker (0 violations), `npm test` (260), `npm run build`, Playwright 57 of 57 on 1280/768/390 with axe. Written only: the 8 Postman requests; `api-ci` and `app-ci` on the PR.
* Accepted limits: (1) the key is stored in its own statement, so a crash between the money commit and marking the key done leaves a running row that is taken over after 60 s (a window of milliseconds); a stricter fix needs the key row inside the money transaction. (2) Only 2xx results are stored (all four POSTs answer 204); a business failure frees the key. (3) A first attempt still running after 20 s makes the waiting request answer 503 (the frontend shows "uncertain"). (4) Review stays available above the balance (feedback only).
* Not done on purpose: deposit, recharge and Pix screens (SL-04, SL-06).

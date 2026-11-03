# SPEC-SL-03 — Withdrawal + shared result panel + idempotency

**Status:** Draft (not authorized) · **Depends on:** SL-02 · **Parallel with:** none
**Branch:** `slice/03-withdrawal` · **Track:** Backend + Frontend + E2E
**Absorbs:** FE-04 (Withdrawal, shared result panel), BE-06 (`Idempotency-Key`, Flyway `V4`), BE-05 (null/invalid body handling for withdrawal).
**Read first:** `../memory/produto.md` (rules 1, 2; ATM withdrawal), `../memory/contexto-tecnico.md` (BE-03 design), `../spec-execution-flow.md` §1b, v2 reference (Withdraw, review dialog, result panel in its three outcomes). Protocol: `../shared/como-executar.md`.

## Objective
The simplest money flow, built first because it also creates what the other three flows reuse: the **review → confirm → result** pattern with a success / business-failure / **uncertain-outcome** panel on the frontend, and the **`Idempotency-Key`** protection on the backend so a retried or double-submitted write can never debit twice.

## Human action before
Approve the idempotency design (defaults below): header optional; same key + same body → replay of the original result; same key + different body → 422 "Idempotency-Key reuse with a different request"; keys scoped per customer; TTL 24 h.

## Scope
**Backend**
- `Idempotency-Key` (optional header, max 64 chars) on **all four POST money endpoints** via one component (Flyway **`V4`**: table keyed by `(subject, key)` with request hash, stored status, `created_at`); the replay returns the stored status/body without touching balance, ledger or outbox. Concurrent identical keys: one wins, the other waits or replays — no double debit.
- Withdrawal: `{amount}` only is the supported body; null body / missing or non-positive amount → 422 with the existing messages (never 500); optional `agency`/`numberAccount` keep their BE-04 semantics (owner's own number else 404).
- Contract: header parameter and replay behaviour documented (preview → approval).

**Frontend**
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
- [ ] HUMAN: approve the idempotency design
- [ ] CONTRACT: preview header + behaviour; wait for approval
- [ ] BE tests first (red): replay with the same key does not double-debit; same key different body → 422; parallel same-key requests debit once; no key behaves as today; null/zero/negative amount → 422 without 500; exact-balance withdrawal → 204
- [ ] BE: `V4` + idempotency component wired to all four POSTs; suite green
- [ ] FE tests first (red): double click → one request; no retry on failure; body is `{amount}` only; key reused only on explicit retry; three result variants render; formatting of the amount
- [ ] FE: result panel + operation shell + Withdraw page + review dialog
- [ ] E2E: withdraw 100 → balance down by 100 and a row appears; exact full balance → balance 0; amount above balance → business-failure panel with unchanged balance; forced network failure → uncertain panel and "Try again" reuses the key (backend debits once); axe on the page and the dialog
- [ ] Postman: withdrawal requests with and without `Idempotency-Key`
- [ ] Docs: result-panel usage in `features/…/doc.md`

## Acceptance
- [ ] Replaying a POST with the same `Idempotency-Key` does not double-debit (proved for withdrawal; the component is reused by SL-04…06)
- [ ] Withdrawal request body is `{amount}` only; double click sends exactly one request; no POST is auto-retried
- [ ] A debit of exactly the full balance succeeds; amount > balance → 422 and balance unchanged

## Minimal validation (required to close this spec)
- [ ] `./mvnw -B verify` green (state the replay and concurrency tests)
- [ ] `npm ci && npm run build && npm test` green; E2E green on 3 viewports; axe no serious violations
- [ ] Browser check: withdraw, see balance and statement row change
- [ ] Both pipelines green on the PR

## Stop and ask
- New table beyond `V4`; 409 vs 422 on key reuse; any change to the success/204 contract

## Skills
java-persistence-performance (transactions-concurrency), java-modern-engineering, openapi-rest-contract, angular-module-scaffold, angular-tailwind-ui-specialist

## Result
_Filled on completion: result, checks run, accepted limitations._

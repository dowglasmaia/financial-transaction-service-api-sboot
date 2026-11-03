# SPEC-SL-04 — Deposit (boleto simulation on top of the real deposit)

**Status:** Draft (not authorized) · **Depends on:** SL-03 · **Parallel with:** none
**Branch:** `slice/04-deposit` · **Track:** Backend + Frontend + E2E
**Absorbs:** FE-04 (Deposit), BE-05 (deposit body validation).
**Read first:** `../memory/produto.md` (rule 7: third-party deposits allowed, agency ignored), `../memory/contexto-tecnico.md` (*prototype vs backend* case), `../spec-execution-flow.md` §1b, v2 reference (Deposit + boleto card). Protocol: `../shared/como-executar.md`.

## Objective
Offer Deposit with the v2 experience (enter an amount → **generate a boleto** → **simulate payment**) while the money movement is the real `POST /transactions/deposit`. This resolves the Tier-2 divergence recorded since FE-01 (proposal A in `docs/prototype-deviations.md`).

## Human action before
Approve the reconciliation (default): the boleto screen is a **client-side simulation clearly labelled as demo**; pressing **"Simulate payment"** sends `POST /transactions/deposit {agency, account, amount}` for the **caller's own account** (agency and number from `/accounts/me`); no boleto is stored or fetched from the server. If the owner prefers a different behaviour, this slice stops.

## Scope
**Backend**
- Endpoint and semantics unchanged (rule 7). Validation hardening: null body, missing/zero/negative amount, blank account → 422 with existing messages, never 500; unknown account → 404 "Account not found." (kept); idempotency from SL-03 applies; Kafka/outbox untouched.
- Contract: examples/enums only if the preview shows drift.

**Frontend**
- **Deposit page:** amount with presets (R$ 100/250/500/1000), validation "Enter an amount greater than zero."; **"Generate boleto"** shows a preview panel, then a boleto card (beneficiary "Maia Bank (demo)", payer from the ID token, document id, due date, amount, decorative barcode and digitable line) with actions **Download boleto** / **Share** / **Copy line** (toast "Demo action" — no real file or link), **Simulate payment** and **New amount**; footer "Demo. This is not a real boleto."
- "Simulate payment" runs the shared operation shell + result panel from SL-03: success "Deposit registered" (balance and a `DEPOSIT` row refresh), 404 on the account → field-level error, 422 → backend message, uncertain outcome as in SL-03.
- Home "Deposit" tile and nav entry active.

**Out of scope:** depositing into another customer's account from the UI (the API still allows it), real boleto issuing/reconciliation.

## Tasks
- [ ] HUMAN: approve the boleto-simulation reconciliation
- [ ] BE tests first (red): null/invalid bodies → 422 (no NPE); third-party deposit still works and the receiver's statement shows it; replay with the same key credits once
- [ ] BE: validation fixes; suite green
- [ ] FE tests first (red): amount validation; boleto generation is deterministic from the amount; "Simulate payment" sends one request with the caller's agency/account/amount; double click → one request; no retry
- [ ] FE: Deposit page, boleto card, result variants
- [ ] E2E: generate → simulate → balance up by exactly the amount and a `DEPOSIT` row; invalid amount blocked; forced network failure → uncertain panel; axe
- [ ] Postman: deposit requests and the third-party case
- [ ] Docs: close proposal A in `docs/prototype-deviations.md`

## Acceptance
- [ ] The balance increases by exactly the amount, once, and the statement shows a `DEPOSIT` row
- [ ] UI never claims a real boleto exists; demo wording on every boleto surface
- [ ] No auto-retry; double click sends one request

## Minimal validation (required to close this spec)
- [ ] `./mvnw -B verify` green; `npm ci && npm run build && npm test` green; E2E green on 3 viewports; axe no serious violations
- [ ] Browser check of the full flow
- [ ] Both pipelines green on the PR

## Stop and ask
- Any server-side boleto, new endpoint or table; changing who can be credited

## Skills
java-modern-engineering, angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max

## Result
_Filled on completion: result, checks run, accepted limitations._

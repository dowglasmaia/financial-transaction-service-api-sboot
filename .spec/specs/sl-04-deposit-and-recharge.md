# SPEC-SL-04 — Deposit and mobile recharge (one slice, two pages)

**Status:** Draft (not authorized; merged with the former SL-05 on 2026-11-04) · **Depends on:** SL-03 · **Parallel with:** none
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
- [ ] HUMAN: approve the boleto-simulation reconciliation
- [ ] BE tests first (red) — deposit: unknown account 404, third-party deposit visible in the receiver's statement, replay with the same key credits once
- [ ] BE tests first (red) — recharge: invalid phone / unknown carrier / non-positive amount → 422 (no NPE); payload bytes equal the characterization; debit + ledger + outbox in one transaction; replay with the same key debits once
- [ ] BE: fixes only where a red test demands; `./mvnw -B verify` green; `check-architecture.mjs --strict` 0 violations
- [ ] FE tests first (red) — deposit: amount validation; boleto deterministic from the amount; "Simulate payment" sends one request with the caller's agency/account/amount; double click → one request; no retry
- [ ] FE: Deposit page, boleto card, result variants (own commits)
- [ ] FE tests first (red) — recharge: phone mask and area-code/number split; carrier selection; copy says "requested"; one request on double click; no retry
- [ ] FE: Recharge page, carrier cards, Home quick-recharge card, result variants (own commits)
- [ ] E2E: deposit generate → simulate → balance up by exactly the amount and a `DEPOSIT` row; recharge 30 → balance −30, `PHONE_RECHARGE` row, copy contains "requested" and not "completed"; invalid amount/phone blocked; forced network failure → uncertain panel; axe on both pages
- [ ] Manual: the event appears on `recharge-mobile-topic` in Kafdrop (`http://localhost:19000`); record what was seen
- [ ] Postman: deposit (incl. third-party) and recharge requests
- [ ] Docs: close proposal A and update the carrier row in `docs/prototype-deviations.md`

## Acceptance
- [ ] Deposit: the balance increases by exactly the amount, once, and the statement shows a `DEPOSIT` row; the UI never claims a real boleto exists (demo wording on every boleto surface)
- [ ] Recharge: success text says "requested", never "completed"; Kafka payload and topic identical to the characterization; balance, ledger and outbox stay consistent when the request fails halfway (the failure-injection tests stay green)
- [ ] No auto-retry; double click sends one request (both pages)

## Minimal validation (required to close this spec)
- [ ] `./mvnw -B verify` green; `npm ci && npm run build && npm test` green; E2E green on 3 viewports (1280/768/390); axe no serious violations
- [ ] Browser check of both full flows + Kafdrop observation
- [ ] Both pipelines green on the PR

## Stop and ask
- Any server-side boleto, new endpoint or table; changing who can be credited; any change to the Kafka topic, key, headers or payload; adding carriers

## Skills
java-modern-engineering, java-spring-create-module (only if a use case changes), angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max

## Result
_Filled on completion: result, checks run, accepted limitations._

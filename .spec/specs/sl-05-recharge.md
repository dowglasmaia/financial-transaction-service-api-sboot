# SPEC-SL-05 — Mobile recharge (requested, never "completed")

> **Do not execute this spec on its own.** Merged into **SL-04** (`sl-04-deposit-and-recharge.md`) on 2026-11-04: after SL-03 the recharge page is almost only frontend and shares every piece with Deposit. The text below is kept as the scope catalogue.

**Status:** Superseded by SL-04 · **Depends on:** SL-03 · **Parallel with:** none
**Branch:** `slice/05-recharge` · **Track:** Backend + Frontend + E2E
**Absorbs:** FE-04 (Recharge), BE-05 (recharge validators, null bodies).
**Read first:** `../memory/produto.md` (rules 4, 6: carriers, outbox, byte-identical Kafka payload), `../memory/contexto-tecnico.md` (BE-03 outbox), `../spec-execution-flow.md` §1b, v2 reference (Recharge page, Home quick-recharge card). Protocol: `../shared/como-executar.md`.

## Objective
Recharging a mobile number from the browser: the debit and the ledger row are immediate, the carrier credit is **asynchronous** (event on Kafka via the transactional outbox), so the UI promises only that the recharge was **requested**.

## Human action before
None (decisions fixed: carriers are the backend enum `VIVO | CLARO | TIM | ALGAR`; v2's "Oi" becomes Algar — deviation #3).

## Scope
**Backend**
- Validator fixes (operator-precedence NPE, F-11): null/blank/short phone, area code, unknown carrier, non-positive amount → 422 with existing messages; phone rule unchanged (`^[1-9]{2}9[0-9]{8}$` over area code + number); idempotency from SL-03 applies.
- **Kafka topic and payload stay byte-identical** (BE-03 characterization must still pass); outbox row written with the debit.
- Contract: enum documented as the four carriers.

**Frontend**
- **Recharge page:** carrier cards (Claro, Vivo, TIM, Algar — brand-colored wordmarks as text, no logos), masked phone input `(11) 9 1234-5678` that splits into `areaCode` + `phoneNumber` for the request, value presets (R$ 15/30/50/100) + custom amount, same balance feedback as SL-03, review dialog showing carrier / number / amount, result variants. Success copy: **"Recharge requested. Your carrier will apply the credit shortly."** — never "completed".
- **Home quick-recharge card** (from v2) reuses the same form component and opens the same review dialog.
- Nav entry "Recharge" active.

**Out of scope:** recharge history screen, carrier lookup, more carriers.

## Tasks
- [ ] BE tests first (red): invalid phone/carrier/amount/nulls → 422 (no NPE); debit + ledger + outbox in one transaction; payload bytes equal the BE-01/BE-03 characterization; replay with the same key debits once
- [ ] BE: validator fixes; suite green
- [ ] FE tests first (red): phone mask and area-code/number split; carrier selection; copy says "requested"; one request on double click; no retry
- [ ] FE: Recharge page, carrier cards, Home quick-recharge card, result variants
- [ ] E2E: recharge 30 → balance −30, `PHONE_RECHARGE` row, copy contains "requested" and not "completed"; invalid phone blocked; uncertain outcome; axe
- [ ] Manual check: the event appears on `recharge-mobile-topic` in Kafdrop (`http://localhost:19000`); record what was seen
- [ ] Postman: recharge requests

## Acceptance
- [ ] Success text says "requested", never "completed"
- [ ] Kafka payload and topic identical to the BE-01 characterization
- [ ] Balance, ledger and outbox stay consistent when the request fails halfway (existing BE-03 failure-injection tests still green)

## Minimal validation (required to close this spec)
- [ ] `./mvnw -B verify` green; `npm ci && npm run build && npm test` green; E2E green on 3 viewports; axe no serious violations
- [ ] Browser check + Kafdrop observation
- [ ] Both pipelines green on the PR

## Stop and ask
- Any change to the Kafka topic, key, headers or payload; adding carriers

## Skills
java-modern-engineering, angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max

## Result
_Filled on completion: result, checks run, accepted limitations._

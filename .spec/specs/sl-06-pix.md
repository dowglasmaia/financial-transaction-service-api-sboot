# SPEC-SL-06 — Pix (three steps, with the external MaiaBank call)

**Status:** Draft (not authorized) · **Depends on:** SL-05 · **Parallel with:** none
**Branch:** `slice/06-pix` · **Track:** Backend + Frontend + E2E
**Absorbs:** FE-04 (Pix), BE-05 (CPF/e-mail/phone validators, null bodies, F-11).
**Read first:** `../memory/produto.md` (rules 3, 5: key formats, reserve → MaiaBank → confirm/compensate, no local credit), `../memory/contexto-tecnico.md` (F-09, BE-03 Pix design), `../spec-execution-flow.md` §1b, v2 reference (Pix steps 1-3, key types, review, result). Protocol: `../shared/como-executar.md`.

## Objective
The most delicate flow: send a Pix by key from the browser, with validation that mirrors the server, a clear review step, and honest outcomes — sent, not sent (balance untouched), or **not confirmed** (check the statement before trying again).

## Human action before
Provide the **MaiaBank contract** only if the Pix payload must change (F-09: `receiversPix()` is an empty POST today and key existence is a stub; both stay as they are unless the owner decides otherwise). Confirm `maiabank-api` is up on `localhost:8089`.

## Scope
**Backend**
- Validator fixes (F-11): CPF length, repeated digits and check digits; e-mail regex; phone rule; null body / missing `pixKey`, `keyType`, `transactionAmount` → 422 with existing messages (no NPE/500); idempotency from SL-03 applies to the whole reserve → call → confirm/compensate sequence.
- Behaviour unchanged: reserve (debit + `PENDING` row) → MaiaBank → confirm (`COMPLETED`) or compensate (credit back, `FAILED`); no local recipient credited; Feign timeouts and circuit breaker as built in BE-03.
- Contract: key type enum and error statuses documented.

**Frontend**
- **Pix page, three steps with the stepper** (Key → Amount → Review), then the result panel:
  1. Key type segmented control (CPF / E-mail / Mobile), masked input per type, inline validation mirroring the server (feedback only; CPF check digits, e-mail, `(DD) 9 XXXX-XXXX`), hint "Check the key with the person you are paying.";
  2. Amount with presets (R$ 50/100/200/500) and "Use full balance"; balance feedback as in SL-03;
  3. Review lines (key type, **masked key**, amount, from "Your account") and "Confirm and send";
  - Result: "Pix sent" with receipt lines; business failure (422, e.g. "Fail send Pix.") → "We couldn't send this Pix. Your balance was not changed."; uncertain outcome → "We couldn't confirm the operation. Check your transactions before trying again, so you don't pay twice."
- The key is never written to logs/console; masked in the review and receipt.
- Nav entry "Pix" and the Home Pix tile active. The ledger only stores the type `PIX` and the sign, so statement rows read "Pix sent" (outgoing) or "Pix received" (incoming) from the sign; no recipient name is invented.

**Out of scope:** recipient lookup, QR codes, Pix schedule/favorites, crediting a local recipient.

## Tasks
- [ ] HUMAN: confirm the MaiaBank stub is up (and provide its contract only if the payload must change)
- [ ] BE tests first (red): CPF edge cases (length, repeated digits, wrong check digits), e-mail, phone, null bodies → 422 with no NPE; MaiaBank 500/timeout leaves balance and ledger consistent (existing tests stay green); replay with the same key sends one Pix
- [ ] BE: validator fixes; suite green
- [ ] FE tests first (red): CPF/e-mail/phone masks and validity rules identical to the backend cases; step navigation and back; review masks the key; one request on double click; no retry; copy for the three outcomes
- [ ] FE: Pix page (stepper, three steps), review, result variants
- [ ] E2E (with the real `maiabank-api` container): send 50 → balance −50 and a `PIX` row; invalid CPF blocked on step 1; amount above balance blocked on step 2; forced failure of the POST → uncertain panel; axe on each step
- [ ] Postman: Pix requests (valid CPF `529.982.247-25` as the documented test key)
- [ ] Docs: update product/technical memory for what changed (F-11 closed)

## Acceptance
- [ ] A valid Pix debits the caller once and appears in the statement; a failed Pix never leaves the customer debited
- [ ] Every client-side rule agrees with the server (same accept/reject on the shared cases)
- [ ] The key never appears in console output or tokens/keys in URLs

## Minimal validation (required to close this spec)
- [ ] `./mvnw -B verify` green; `npm ci && npm run build && npm test` green; E2E green on 3 viewports; axe no serious violations
- [ ] Browser check of success and failure paths against the compose stack
- [ ] Both pipelines green on the PR

## Stop and ask
- Any change to the MaiaBank payload/endpoint or to the reserve → confirm/compensate behaviour; crediting a local recipient

## Skills
java-modern-engineering, java-persistence-performance, angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max

## Result
_Filled on completion: result, checks run, accepted limitations._

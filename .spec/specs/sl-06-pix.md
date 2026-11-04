# SPEC-SL-06 — Pix (three steps, with the external MaiaBank call)

**Status:** Implemented on `slice/06-pix`, awaiting the owner's PR, review and green pipelines (revised 2026-11-04) · **Depends on:** SL-04 (deposit + recharge) · **Parallel with:** none
**Branch:** `slice/06-pix` · **Track:** Backend + Frontend + E2E
**Absorbs:** FE-04 (Pix), BE-05 (CPF/e-mail/phone rules, F-11). The null/blank body *shape* checks of the Pix request were done in SL-03.
**Read first:** `../memory/produto.md` (rules 3, 5: key formats, reserve → MaiaBank → confirm/compensate, no local credit), `../memory/contexto-tecnico.md` (F-09, BE-03 Pix design), `../spec-execution-flow.md` §1b, v2 reference (Pix steps 1-3, key types, review, result). Protocol: `../shared/como-executar.md`.

## Objective
The most delicate flow: send a Pix by key from the browser, with validation that mirrors the server, a clear review step, and honest outcomes — sent, not sent (balance untouched), or **not confirmed** (check the statement before trying again).

## Human action before
Provide the **MaiaBank contract** only if the Pix payload must change (F-09: `receiversPix()` is an empty POST today and key existence is a stub; both stay as they are unless the owner decides otherwise). Confirm `maiabank-api` is up on `localhost:8089`.

## Scope
**Backend**
- Rule fixes (F-11), in `transaction.domain` (`CPFValidator`, `EmailValidator`, `PhoneValidator`, used through the value object `PixKey`): CPF length, repeated digits and check digits; e-mail regex; phone rule — the existing characterization tests that pin today's behaviour are updated on purpose and the change is listed in the Result; the error message keeps the lower-case key type (`PixKeyType.label()`, e.g. "…specified type: cpf"); idempotency from SL-03 applies to the whole reserve → call → confirm/compensate sequence.
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
- [x] HUMAN: confirm the MaiaBank stub is up (and provide its contract only if the payload must change)
> ✅ 2026-11-04 — `maiabank-api` is up on `localhost:8089` and the e2e runs against it; the payload and endpoint did NOT change. Verification: `docker ps`, e2e sends.
- [x] BE tests first (red): CPF edge cases (length, repeated digits, wrong check digits), e-mail, phone → 422 with no NPE; MaiaBank 500/timeout leaves balance and ledger consistent (existing tests stay green); replay with the same key sends one Pix
> ✅ 2026-11-04 — `PixKeyRulesTest` (64 cases), `PixKeyRulesApiTest` (20: 15 bad keys → 422 with the existing message, nothing moves, MaiaBank never called; 4 good keys; replay sends ONE Pix), flipped characterizations. Red: 25 failures + 4 errors of 92 (commit 2bb569d). The MaiaBank 500/timeout tests (`PixMaiaBankResilienceTest`, `FailureInjectionTest`) stayed green.
- [x] BE: validator fixes; suite green
> ✅ 2026-11-04 — strict `CPFValidator`, `EmailValidator`, null-safe validators, `PixKey.masked()` used for the only log line that had the key. A second defect found by the e2e against the real container: `MaiaBankClient` could not decode the `text/plain` answer (red test `aPlainTextAnswerLikeTheRealMaiaBankIsASuccess`, then `ResponseEntity<String>`). Verification: `./mvnw -B verify` (JDK 21) → 371 tests, 0 failures, 1 skipped; strict checker → 84 files, 0 violations, 1 warning (W04); `ArchitectureTest` green.
- [x] FE tests first (red): CPF/e-mail/phone masks and validity rules identical to the backend cases; step navigation and back; review masks the key; one request on double click; no retry; copy for the three outcomes
> ✅ 2026-11-04 — `pix-key.spec.ts` (same case lists as the backend), `pix.api.spec.ts`, `pix-page.spec.ts` (steps, masks, masked review, one request, no retry, three outcomes, nothing in the console), Home tile. Red: modules did not resolve (commit f2dab3d). Verification: `npm test` → 379 passed.
- [x] FE: Pix page (stepper, three steps), review, result variants
> ✅ 2026-11-04 — Pix page, API client, nav entry, Home tile; `npm run build` no warnings (739 kB initial).
- [x] E2E (with the real `maiabank-api` container): send 50 → balance −50 and a `PIX` row; invalid CPF blocked on step 1; amount above balance blocked on step 2; forced failure of the POST → uncertain panel; axe on each step
> ✅ 2026-11-04 — `pix.e2e.ts` (7) × 3 viewports against the real `maiabank-api`: send 50 → balance 4950, one COMPLETED `PIX` row, one POST with the plain key, double click = 1 request; invalid CPF/e-mail/mobile blocked on step 1; amount above the balance blocked on step 2; lost answer → uncertain → Try again with the same key → debited once; network down → 1 request; 422 'Fail send Pix.' panel; exact balance sendable; axe on every step and panel. Verification: full `npx playwright test` → 113/114, the one failure was the first test of the run with a cold API and passes on rerun (3/3).
- [x] Postman: Pix requests (valid CPF `529.982.247-25` as the documented test key)
> ⚠️ 2026-11-04 — folder "SL-06 Pix" (9 requests, test key 529.982.247-25) written, NOT executed. The same cases are proven by the backend tests and the e2e.
- [x] Docs: update product/technical memory for what changed (F-11 closed)
> ✅ 2026-11-04 — `contexto-tecnico.md` (F-11 closed, D-58), `produto.md` (rule 3), `docs/prototype-deviations.md` rows 40–46.

## Acceptance
- [x] A valid Pix debits the caller once and appears in the statement; a failed Pix never leaves the customer debited
> ✅ e2e: 5000 → 4950 once (psql), one COMPLETED row, statement shows 'Pix sent'; a failed Pix never leaves the customer debited (BE-03 tests + `aPlainText…`/500/timeout tests).
- [x] Every client-side rule agrees with the server (same accept/reject on the shared cases)
> ✅ the same case lists in `PixKeyRulesTest` (Java) and `pix-key.spec.ts` (TypeScript), all passing.
- [x] The key never appears in console output or tokens/keys in URLs
> ✅ unit test spies on the console; e2e checks the page text; the review/receipt are masked; the server logs `PixKey.masked()`; no key in any URL.

## Minimal validation (required to close this spec)
- [x] `./mvnw -B verify` green; `npm ci && npm run build && npm test` green; E2E green on 3 viewports; axe no serious violations
> ⚠️ `./mvnw -B verify` ✅ (371), `npm run build` ✅, `npm test` ✅ (379), e2e ✅ on 1280/768/390, axe ✅. `npm ci` NOT run (dependencies unchanged).
- [x] Browser check of success and failure paths against the compose stack
> ✅ screenshots at 1280 (step 1 with the CPF error) and 390 (review, which showed the cut stepper label, fixed) reviewed with the mock IdP against the real MaiaBank stub. The owner still does the real Auth0 run.
- [ ] Both pipelines green on the PR
> ⏳ not executed: they run on the PR the owner opens.

## Stop and ask
- Any change to the MaiaBank payload/endpoint or to the reserve → confirm/compensate behaviour; crediting a local recipient

## Skills
java-modern-engineering, java-persistence-performance, angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max

## Result
**Result (2026-11-04).** Pix in three steps with honest outcomes, the Pix key rules fixed (F-11) and a real MaiaBank defect found and fixed.
* **Backend:** strict `CPFValidator` (11 digits, plain or masked, not all equal, check digits), stricter `EmailValidator`, null-safe validators (never an exception), `PixKey.masked()` for logs. Reserve → MaiaBank → confirm/compensate, Feign timeouts and the circuit breaker are untouched; the idempotency of SL-03 covers the whole sequence (replay sends ONE Pix).
* **Defect found by the e2e against the real `maiabank-api`:** it answers `200 text/plain "API-ON"` and the client declared `ResponseEntity<Void>`, so every real Pix failed to decode, was compensated and opened the breaker (the tests mocked MaiaBank). Fixed with `ResponseEntity<String>`; the request, the URL and the saga did not change (no "Stop and ask" item touched; flagged for the owner anyway).
* **Characterization tests changed on purpose (F-11):** `cpfWithOneCharacterThrows` (now: rejected, no exception), `cpfWithRepeatedDigitsIsAccepted` (now: rejected), `emailRegex` (+ `a..b@example.com` rejected).
* **Frontend:** `shared/util/pix-key.ts` (rules, masks, feedback, same case lists as the backend), Pix page with the stepper, masked review and receipt, the three outcomes, Home Pix tile, nav entry; stepper shows only the current label on phones.
* **Checks run:** backend 371 tests (JDK 21), strict architecture 0 violations, 379 unit tests, build, e2e 114 (3 viewports, real MaiaBank container) with axe, screenshots.
* **Only written / not run:** the 9 Postman requests, `npm ci`, both pipelines (on the PR), the real Auth0 login (owner).
* **Accepted limits:** a phone key is sent as digits only (the server rejects masked phones, unlike masked CPFs); an uncertain Pix may still have gone through (the UI says to check the statement; the same key makes Try again safe); no "Pix received" rows because no local recipient is credited (D-15); the key-existence check stays a stub (F-09 open); the contract examples were not changed (the enum and the 422 were already documented).

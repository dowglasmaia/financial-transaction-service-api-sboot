# SPEC-FE-04 — Pix, Deposit, Recharge, Withdrawal

**Status:** Draft (not authorized) · **Depends on:** FE-03, BE-03 · **Parallel with:** none
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md` (incl. *Frontend agent autonomy*) and the visual/flow reference `../../prototype/Maia Bank UI prototype/` (open `Maia Bank.dc.html`). Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Implement the four money flows with review → confirm → result, including the uncertain-outcome state.

## Scope
- Pix stepper (Key → Amount → Review → Result)
- Deposit form (agency informational, 404 → account field error)
- Recharge (carrier cards, masked phone, wording "requested")
- Withdrawal (body contains only `amount`)
- Shared result panel: success / business failure / uncertain outcome; `Idempotency-Key` header

**Out of scope:** Recipient lookup; Non-card withdrawal UI

## Tasks
- [ ] Four modules + `doc.md`
- [ ] Validators mirroring backend rules (feedback only)
- [ ] Result panel variants
- [ ] Tests: double-click sends one request, exact balance allowed, withdrawal body
- [ ] Keyboard + axe

## Acceptance
- [ ] Each flow completes against the compose stack
- [ ] No auto-retry of POSTs
- [ ] Copy matches the brief's copy deck

## Minimal validation (required to close this spec)
- [ ] Each flow completes against `docker compose` (balance changes, new row in statement)
- [ ] Double click sends exactly one request; no automatic retry of any POST
- [ ] Withdrawal request body is `{amount}` only
- [ ] Recharge success text says 'requested', never 'completed'

## Stop and ask
- Any behaviour, contract or scope change beyond this spec

## Skills
angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max

## Result
_Filled on completion: result, checks run, accepted limitations._

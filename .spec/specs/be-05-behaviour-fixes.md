# SPEC-BE-05 — Dates, validation and error handling

**Status:** Superseded (2026-11-03, D-31) — executed through the vertical slices · **Depends on:** BE-04 · **Parallel with:** none
> **Do not execute this spec on its own.** Scope catalogue only. Dates, handler → **SL-02**; validators and null bodies → **SL-03** (withdraw), **SL-04** (deposit), **SL-05** (recharge), **SL-06** (Pix). Mapping in `README.md`.
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Fix the statement date semantics and make validation/errors robust without changing the 204/404/422 contract.

## Scope
- Half-open range `[start 00:00, end+1d 00:00)` in the chosen time zone (record the choice); null/invalid dates → defined default or 422, never 500
- Fix validator operator-precedence NPEs; CPF length/repeated-digit checks
- `HandlerException`: parse errors, generic `Exception` (500 with `{code,message}`), use or remove the unused `HttpStatus` field; assess removing `@EnableWebMvc`

**Out of scope:** Bean Validation switch and contract (BE-06)

## Tasks
- [ ] Tests: single-day range, null dates, bad format, null bodies
- [ ] Implement date conversion (input stays `dd/MM/yyyy`)
- [ ] Fix validators
- [ ] Extend handler; verify JSON/actuator defaults
- [ ] CPF edge-case tests

## Acceptance
- [ ] Single-day query returns that day's rows
- [ ] No validation path returns 500/NPE
- [ ] All existing messages unchanged

## Minimal validation (required to close this spec)
- [ ] A statement for a single day returns that day's transactions
- [ ] Missing/invalid dates and null request bodies never produce 500/NPE
- [ ] Suite green

## Stop and ask
- Time-zone choice

## Skills
java-modern-engineering

## Result
_Filled on completion: result, checks run, accepted limitations._

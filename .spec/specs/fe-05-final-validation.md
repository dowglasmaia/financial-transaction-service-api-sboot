# SPEC-FE-05 — End-to-end, accessibility and final review

> **Do not execute this spec on its own.** Folded into **FINAL-01** on 2026-11-04: every slice already ships its own e2e and axe checks, so the rest (flows F-0…F-9 matrix, responsive audit, demo README with screenshots, memory sync) is part of the final test round (`final-01-test-round.md`, items E, F, H, J). The text below is kept as the scope catalogue.

**Status:** Superseded by FINAL-01 · **Depends on:** SL-06, BE-07
> Since 2026-11-03 each slice (SL-01…SL-06) already ships its own Playwright e2e and axe checks; this spec completes the matrix (flows F-0…F-9 × 3 viewports), the responsive/a11y audit, the demo README and the memory update.
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md` (incl. *Frontend agent autonomy*) and the visual/flow reference `../../prototype/Maia Bank UI prototype/` (open the **v2** file `Maia Bank v2.dc.html` (the v1 file is history)). Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Prove everything works together and the docs match the result.

## Scope
- Playwright e2e on Docker compose with mock issuer for all flows
- axe in e2e; 1280/768/390 checks (plus a manual 320 pass); README for running the demo
- Update `memory/` to match what was delivered

**Out of scope:** New features

## Tasks
- [ ] E2E flows
- [ ] Accessibility + responsive audit
- [ ] Demo README + screenshots
- [ ] Update memory files

## Acceptance
- [ ] All flows pass; zero serious axe violations
- [ ] memory/ reflects reality

## Minimal validation (required to close this spec)
- [ ] Playwright e2e for flows F-0…F-9 green on 3 viewports
- [ ] axe: zero serious violations
- [ ] Demo README with run instructions exists

## Stop and ask
- Release sign-off

## Skills
ui-ux-pro-max

## Result
_Filled on completion: result, checks run, accepted limitations._

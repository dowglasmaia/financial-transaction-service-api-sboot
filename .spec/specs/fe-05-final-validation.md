# SPEC-FE-05 — End-to-end, accessibility and final review

**Status:** Draft (not authorized) · **Depends on:** FE-04, BE-07 · **Parallel with:** BE-08
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md` (incl. *Frontend agent autonomy*) and the visual/flow reference `../../prototype/Maia Bank UI prototype/` (open `Maia Bank.dc.html`). Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Prove everything works together and the docs match the result.

## Scope
- Playwright e2e on Docker compose with mock issuer for all flows
- axe in e2e; 320/768/1280 checks; README for running the demo
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

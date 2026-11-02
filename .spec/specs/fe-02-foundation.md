# SPEC-FE-02 — Angular foundation: scaffold, core, design system

**Status:** Draft (not authorized) · **Depends on:** BE-02, FE-01 · **Parallel with:** BE-04
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md` (incl. *Frontend agent autonomy*) and the visual/flow reference `../../prototype/Maia Bank UI prototype/` (open `Maia Bank.dc.html`). Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Create the Angular 21 project and the shared base every module uses.

## Scope
- Scaffold with `angular-frontend-scaffold`: project name **`app-financial-transaction-service`**, created at the repo root (`--output <repo root>`); keep its defaults (zoneless, strict, port 4200). `app-ci.yml` (MONO-01) starts building it automatically
- Core: runtime `config.json`, `API_BASE_URL`, error interceptor, auth interceptor (dev-open until BE-04), layout shell, dev proxy to `localhost:8088`, OpenAPI type generation
- Design tokens **extracted from the prototype** (palette, type, radius, spacing, motion; dark default + light), theme toggle, shared components from the prototype, `/design` route; test runner decision; `docs/prototype-deviations.md` created

**Out of scope:** Feature modules

## Tasks
- [x] HUMAN: project name/location
  > ✅ 2026-11-02 16:00 +0000 — decided by the owner: `app-financial-transaction-service` at the repo root (monorepo).
- [ ] Run scaffold + `validate.mjs`
- [ ] Core pieces
- [ ] Tokens + themes
- [ ] Shared components with all states + axe
- [ ] Choose test runner and chart library

## Acceptance
- [ ] `npm run build` passes
- [ ] Contrast passes in both themes
- [ ] No HTTP outside `data-access`; no `any`

## Minimal validation (required to close this spec)
- [ ] `cd app-financial-transaction-service && npm ci && npm run build` succeeds
- [ ] `app-ci` runs and is green on the PR
- [ ] `/design` route shows tokens and components in dark and light
- [ ] No `any`, no HTTP outside `data-access`

## Stop and ask
- Test runner / chart library
- New libraries

## Skills
angular-frontend-scaffold, angular-tailwind-ui-specialist, angular-architect

## Result
_Filled on completion: result, checks run, accepted limitations._

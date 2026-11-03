# SPEC-FE-02 — Angular foundation: scaffold, core, design system

**Status:** Done (PR #5 merged 2026-11-02; app-ci green) · **Depends on:** BE-02, FE-01 · **Parallel with:** BE-04
> Addendum 2026-11-03: the v1 look built here (brown/gold tokens) is **replaced by the v2 identity in SL-00**. Structure, core, interceptors, config loader, runner and tests stay.
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
- [x] Run scaffold + `validate.mjs`
  > ✅ 2026-11-02 17:59 +0000 — `scaffold.mjs --output . --project-name app-financial-transaction-service` then `validate.mjs`: exit 0, production build OK (committed as `a4d4e9a`). Angular 21.2, zoneless, strict, Tailwind 4, port 4200 kept.
- [ ] Core pieces
  > ⏳ 2026-11-02 17:59 +0000 — Done: runtime `public/config.json` + validated loader (`core/config`), `API_BASE_URL` from config, interceptors `request-id` → `auth` (dev-open: no token, nothing added) → `api-error` (typed `ApiError{status,code,message,requestId}`; network = status 0 / `NETWORK`), layout shell (skip link, header with demo chip, theme toggle, main/footer landmarks), dev proxy `/api → localhost:8088`. Verification: 20+ unit specs (config, interceptors) green. **Not done: OpenAPI type generation** — it needs the corrected contract (BE-06); deferred to FE-03. Note: the only `fetch` is the `config.json` loader in `core/config` (infrastructure, not a data API).
- [x] Tokens + themes
  > ✅ 2026-11-02 17:59 +0000 — Tokens extracted from the prototype's theme map into `src/styles.scss` (dark default + light; Tailwind `@theme inline` names `bg-surface`, `text-content`, `text-income`…); `ThemeService` with guarded storage; toggle in the header. Verification: `tokens-contrast.spec.ts` asserts WCAG pairs in both themes (written red first: 4 failures for the missing on-brand tokens, then green); visual check in the browser: dark body `rgb(17,12,9)`, light `rgb(246,242,234)`, preference persisted across reload. Deviations logged in `app-financial-transaction-service/docs/prototype-deviations.md` (dark `line-input` fixed to 3:1; added `on-brand` tokens after seeing the light header).
- [ ] Shared components with all states + axe
  > ⏳ 2026-11-02 17:59 +0000 — Built `appButton` (primary/secondary/ghost, disabled, loading), `app-form-field` + `appFieldControl` (hint/error/disabled, `aria-describedby`/`aria-invalid`), `app-money-input`, `app-chip`, `app-alert` (4 tones), `app-skeleton`, `app-empty-state`, `app-kpi-tile`, `app-balance-card`, `app-ledger-row`, `app-theme-toggle`, shell. Verification: `/design` rendered in the built-in browser in dark and light; DOM audit via script: 0 buttons/links without an accessible name, 0 unlabeled inputs, aria-describedby targets exist, heading order h1→h2→h3, header/main/footer landmarks, Manrope/Sora/Phosphor fonts loaded. **axe-core was NOT run** (not installed; planned with Playwright in FE-05). Keyboard focus ring is defined globally but tab-order was not exercised by hand. Dialog/focus-trap components deferred to FE-04 (not needed yet).
- [x] Choose test runner and chart library
  > ✅ 2026-11-02 17:59 +0000 — Test runner: **Vitest** through Angular's `@angular/build:unit-test` (`npm test`; jsdom). Chart library: **none** — the prototype's charts are hand-made SVG/CSS, Home builds them the same way. Recorded in `docs/prototype-deviations.md`. Added dependencies (flag for the owner): `@phosphor-icons/web` (design-mandated icon font), dev: `vitest`, `jsdom`, `@types/node`.

## Acceptance
- [x] `npm run build` passes
  > ✅ 2026-11-02 17:59 +0000 — `rm -rf node_modules dist .angular && npm ci && npm run build`: success, initial bundle 424.51 kB (94.31 kB transfer), budgets OK.
- [x] Contrast passes in both themes
  > ✅ 2026-11-02 17:59 +0000 — 60/60 unit tests green, including 2 themes × 20 token pairs (text ≥ 4.5:1, control borders ≥ 3:1).
- [x] No HTTP outside `data-access`; no `any`
  > ✅ 2026-11-02 17:59 +0000 — `grep` for `any`/`as any`/`$any` in `src`: none; no `HttpClient` use outside `app.config.ts` (provider only); no `data-access` folders exist yet.

## Minimal validation (required to close this spec)
- [x] `cd app-financial-transaction-service && npm ci && npm run build` succeeds
  > ✅ 2026-11-02 17:59 +0000 — EXECUTED (see acceptance item above).
- [x] `app-ci` runs and is green on the PR
  > ✅ 2026-11-02 — `app-ci` success on the PR (48230d5, public Actions API) and on `main` (f8850e9). Earlier note kept: ⛔ 2026-11-02 17:59 +0000 — Not executed: needs the PR (owner opens it). `app-ci` now runs `npm ci`, `npm run build` and `npm test`. Para desbloquear: green `app-ci` on the PR.
- [x] `/design` route shows tokens and components in dark and light
  > ✅ 2026-11-02 17:59 +0000 — EXECUTED in the built-in browser against `ng serve` (dev server stopped afterwards): tokens grid, typography, buttons, form fields, chips, alerts, balance/KPI/ledger, loading and empty states rendered in both themes; theme persisted via `localStorage`.
- [x] No `any`, no HTTP outside `data-access`
  > ✅ 2026-11-02 17:59 +0000 — see acceptance item above.

## Stop and ask
- Test runner / chart library
- New libraries

## Skills
angular-frontend-scaffold, angular-tailwind-ui-specialist, angular-architect

## Addendum (owner request)
Portfolio credit footer replicated from `check-in-api-kotlin/app-check-in-api-angular` (`SiteFooterComponent` + `app-tag`), 9 new unit tests (69 total, green), initial bundle 434.76 kB, visually checked in dark and light. Logged as deviation #8/#9.

## Result
Angular 21 foundation in place: scaffold (skill), runtime config, typed HTTP boundary (request id → auth → error), design tokens in two themes with a contrast test, theme service, 12 shared UI pieces, shell and a `/design` living style guide; Vitest runner with 60 tests. Executed: clean `npm ci` + build, unit tests, visual check of `/design` in dark and light. **Closed:** PR #5 merged, `app-ci` green. **Carried over:** axe-core run (FE-05), OpenAPI type generation (after BE-06), formal sign-off of the prototype (FE-01). **Needs the owner's OK:** new dependencies (`@phosphor-icons/web`, `vitest`, `jsdom`, `@types/node`) and the decisions Vitest / no chart library. Deviations from the prototype are in `app-financial-transaction-service/docs/prototype-deviations.md`.

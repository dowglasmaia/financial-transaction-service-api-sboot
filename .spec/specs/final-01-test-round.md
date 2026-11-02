# SPEC-FINAL-01 — Final test round

**Status:** Draft (not authorized) · **Depends on:** all other specs Done (MONO-01, BE-01…BE-08, FE-01…FE-05) · **Parallel with:** none
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md`.

## Objective
After every spec is finished, run one structured test round over the whole product (API + web app + local stack) and record the evidence. No new features.

## Scope
A. **Backend regression:** `cd api-financial-transaction-service && ./mvnw -B verify` (unit + Testcontainers integration + ArchUnit), coverage report reviewed.
B. **Stack smoke:** `docker compose up --build`; `/actuator/health` UP; Kafka topic exists; seed/test user linked.
C. **API black-box:** every endpoint in `memory/produto.md` called with curl/Postman (Postman collection updated): happy path + each documented error; 401/403/404 matrix; idempotent replay; pagination.
D. **Money integrity:** concurrent debits, Pix with MaiaBank down, recharge with Kafka down — balance/ledger consistent.
E. **Frontend e2e (Playwright):** flows F-0…F-9 at 360/768/1280, dark and light.
F. **Accessibility:** axe on every screen; keyboard-only completion of each money flow.
G. **Security checks:** no secrets in repo (secret scan), dependency scan result, no tokens/PII in logs or URLs, CORS limited to the app origin.
H. **Rules checklist:** each rule in `memory/produto.md` ticked against a test or a manual check.
I. **Exploratory pass (manual, 30 min):** odd inputs, refresh mid-flow, back button, expired session, slow network.

**Out of scope:** new features, performance benchmarking beyond sanity checks.

## Tasks
- [ ] A Backend regression run and coverage review
- [ ] B Stack smoke
- [ ] C API black-box matrix
- [ ] D Money-integrity scenarios
- [ ] E Frontend e2e on 3 viewports × 2 themes
- [ ] F Accessibility and keyboard pass
- [ ] G Security checks
- [ ] H Rules checklist against `memory/produto.md`
- [ ] I Exploratory pass
- [ ] Write `app-financial-transaction-service/docs/test-report.md` (results, evidence, defects found/fixed, accepted limitations)
- [ ] Fix defects found (each with a regression test) and re-run the affected checks

## Acceptance
- [ ] A–I pass, or each failure is fixed or documented as an accepted limitation by the owner
- [ ] `api-ci` and `app-ci` are green on `main`
- [ ] `memory/` is consistent with the delivered system

## Minimal validation
The report exists and every row of the matrix above has a result and evidence.

## Stop and ask
- Any defect that needs a behaviour/contract change; any limitation proposed for acceptance.

## Skills
`java-persistence-performance` (D), `ui-ux-pro-max` (F, pre-delivery checklist), `angular-tailwind-ui-specialist` (ui-review-checklist).

## Result
_Filled on completion._

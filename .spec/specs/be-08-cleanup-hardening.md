# SPEC-BE-08 — Cleanup and portfolio-grade hardening

**Status:** Implemented on `spec/be-07-08-backend-close` together with BE-07, awaiting the owner's PR, review and green pipelines (owner decisions 2026-11-04: reject more than 2 decimals; scans with GitHub-native features) · **Depends on:** BE-07 (and ARCH-02) · **Parallel with:** none
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Tidy internals and add the hygiene a portfolio reviewer expects, inside the feature-module structure created by ARCH-01/02 (D-39).

## Scope
- `Money` value type (decide rounding for >2 decimals); `Account` invariants in `account.domain`; remove dead code (`TransactionRequestDTO`, `AccountModel`, MapStruct leftovers); extend the ArchUnit rules installed by ARCH-01/02 if a new rule is worth freezing
- Logs without PII (mask account/key/CPF, no entity `toString`), correlation id; actuator on separate port; business metrics; Docker healthchecks; dependency + secret scan in CI

**Out of scope:** Moving features between packages or changing the architecture (done in ARCH-01/02; the model is frozen by `ArchitectureTest`); Production compliance

## Tasks
- [x] Money + Account invariants
> ✅ 2026-11-04 — `shared.money.Money` (never negative, at most 2 decimal places, **rejected with 422 "Amount must have at most 2 decimal places", never rounded**) used by `ValidateBodyRequest` for the four POSTs; `Account.create` refuses a null or negative balance. Red first (compile, then behaviour). Verification: `MoneyTest` (14), `MoneyScaleApiTest` (8), `AccountTest` → green.
- [x] Delete dead code
> ✅ 2026-11-04 — removed `TransactionRequestDTO` and `AccountModel` (nothing referenced them; MapStruct leftovers went in BE-07). Verification: build + 410 tests.
- [x] ArchUnit rules reviewed (extend only if needed; keep the legacy list empty)
> ✅ 2026-11-04 — no new rule was worth freezing (`Money` lives in `shared`, imported by `api` and `account.domain` only; the legacy list stays empty). Verification: `ArchitectureTest` green and `check-architecture.mjs --strict` → 84 files, 0 violations, 1 warning (W04).
- [x] Log audit + masking
> ✅ 2026-11-04 — the account number and agency left the deposit, withdrawal and debit log lines (the Pix key was masked in SL-06); `X-Request-Id` is a correlation id in the MDC (`%X{requestId}` in every line) and a response header. Verification: `LogHygieneTest` (a full flow incl. refusals: no account, agency, CPF, phone, e-mail or token in any log text) and `CorrelationIdTest` (4) → green.
- [x] Management port + metrics
> ✅ 2026-11-04 — the actuator listens on **8090** (`management.server.port`, not published by compose); health and Prometheus are open there, the rest of the actuator stays closed, nothing of it is on 8088. Business metrics were not added (no consumer asked for them): the framework metrics are frozen by `MetricNamesBaselineTest`. Verification: `ManagementPortRealServerTest` (real servers, 5) green; in the container: 8090 health UP, 8088 `/actuator/health` → 404, 8090 not reachable from the host.
- [x] CI scans
> ⚠️ 2026-11-04 — written, NOT executed: `.github/dependabot.yml` (maven, npm, docker ×2, actions; weekly + security updates) and `.github/workflows/codeql.yml` (java-kotlin and javascript-typescript). They run on GitHub after the merge. **Owner action:** enable *Secret scanning + Push protection* and *Dependabot alerts* in the repository settings (no workflow can do that).

## Acceptance
- [x] `ArchitectureTest` and `check-architecture.mjs --strict` stay green; no class moved between features
> ✅ see the ArchUnit line; no class moved between features.
- [x] No PII in logs (grep + test)
> ✅ `LogHygieneTest` (test) plus the grep of the `log.` calls in `src/main`.
- [ ] Scans run in CI with no unwaived HIGH/CRITICAL
> ⏳ not executed: they run on GitHub (see the CI line).

## Minimal validation (required to close this spec)
- [x] `check-architecture.mjs --strict` → 0 violations and `ArchitectureTest` green (no new violations introduced)
> ✅ 0 violations, `ArchitectureTest` green.
- [x] Log output of a full flow contains no account number, Pix key or CPF
> ✅ `LogHygieneTest`.
- [x] CI scans pass
> ⚠️ 2026-11-04 — written, NOT executed: `.github/dependabot.yml` (maven, npm, docker ×2, actions; weekly + security updates) and `.github/workflows/codeql.yml` (java-kotlin and javascript-typescript). They run on GitHub after the merge. **Owner action:** enable *Secret scanning + Push protection* and *Dependabot alerts* in the repository settings (no workflow can do that).

## Stop and ask
- Rounding rule (may reject inputs)
- Scan tool choice

## Skills
java-modern-engineering

## Result
_Filled on completion: result, checks run, accepted limitations._

## Result (2026-11-04)
* **Money rule (behaviour change, owner approved):** an amount with more than two decimal places is a **422** on all four POSTs (it was accepted before). The frontend already allowed at most two decimals, so the UI is unaffected; the OpenAPI text of the 422 was not edited (no contract preview was asked for): add the sentence to the contract in the next contract change.
* **Observability:** correlation id (`X-Request-Id`, accepted when it is 1 to 64 safe characters, else generated), actuator on its own port, Docker health checks (API and PostgreSQL; the web container now waits for a healthy API).
* **Hygiene:** `@MockitoBean`, dead code removed, no personal data in logs.
* **Checks run:** 410 backend tests on JDK 25, strict architecture 0 violations, Docker image smoke, Playwright 114/114 against the new container. **Only written / not run:** Dependabot and CodeQL (GitHub), `api-ci` on a pull request.
* **Accepted limits:** business metrics were not invented; the actuator health endpoint is anonymous but shows no details; Prometheus on 8090 has no authentication (private port, never published).

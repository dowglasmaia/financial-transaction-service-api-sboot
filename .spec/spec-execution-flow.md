# Spec Execution Flow

How to execute the 13 specs, in what order, and how to verify each one. Goal: **fix the backend** (BE-01…BE-08) and **build the complete Angular frontend** (FE-01…FE-05). Protocol for ticking tasks and writing evidence: `shared/como-executar.md`. Base context for every run: `memory/produto.md` + `memory/contexto-tecnico.md`.

## 0. Repository layout
Monorepo: `api-financial-transaction-service/` (backend), `app-financial-transaction-service/` (frontend, created by FE-02), `prototype/` (UI reference), `.spec/`, `.github/workflows/` with **two isolated pipelines** (`api-ci.yml`, `app-ci.yml`, path-filtered). Each spec runs inside its own app folder and must keep its pipeline green without touching the other app.

## 1. The loop (same for every spec)
1. **Gate check** — all dependencies are Done; the spec's human actions are completed; spec status set to *Approved* by the owner (a Draft is never executed).
2. **Prepare** — new branch `spec/<id>-<slug>` from the up-to-date `main`; read memory + the spec; run the test suite once to record the baseline.
3. **Test first** — for each behaviour change write/adjust the failing test (characterization → flip), watch it fail.
4. **Implement the smallest change** for one task; commit (`<id>: <task>`); run the suite.
5. **Verify + evidence** — run the spec's checks; tick the task; add the evidence line under it.
6. **Stop at gates** — any contract/behaviour/scope/dependency change not in the spec → stop and ask.
7. **Close** — all acceptance boxes ticked with proof, fill *Result*, set status *Done* in `specs/README.md`, open one PR per spec (CI green), merge, delete the branch.
8. **Sync memory** — if reality diverged from `memory/`, update it in the same PR.

Ready-made prompts per spec: `prompts.md`. **Every spec has a *Minimal validation* block; a spec cannot be closed until it passes (and unexecuted checks are reported as not executed).** After all specs are Done, run `FINAL-01` (final test round).

Generic prompt (the per-spec versions in `prompts.md` add the extras):
> Execute SPEC-<ID> (`.spec/specs/<file>`). Read `.spec/memory/produto.md` and `.spec/memory/contexto-tecnico.md` first, then follow `.spec/spec-execution-flow.md` §4 for this spec. Stay in scope, tests first, tick tasks only with evidence, stop at the "Stop and ask" gates.

## 2. Master order
| # | Spec(s) | Track | Human action before | Exit proof |
|---|---|---|---|---|
| 0 | **MONO-01** monorepo + isolated CI (executed) | both | push to GitHub to verify path filters | both workflows trigger independently |
| 1 | **BE-01** safety net ∥ **FE-01** prototype review | BE ∥ FE | — (prototype already delivered in `prototype/`) | `api-ci` green with all rules characterized ∥ prototype signed off + divergence list |
| 2 | **BE-02** foundation (profiles, secrets, Flyway) | BE | rotate committed DB credentials | app boots on empty DB via Flyway, data survives restart |
| 3 | **BE-03** money integrity ∥ **FE-02** foundation | BE ∥ FE | FE-01 signed off (tokens tasks) — project name is fixed: `app-financial-transaction-service` | concurrency test green; `npm run build` green |
| 4 | **BE-04** Okta + ownership | BE | create Okta org/app/users (see `auth-okta.md`) | 401/403/404 matrix tests; two-user isolation |
| 5 | **BE-05** dates, validation, errors | BE | choose day-boundary time zone | single-day statement works; no 500/NPE |
| 6 | **BE-06** contract + API evolution | BE | approve the OpenAPI preview | contract = behaviour; `/accounts/me`, row ids, pagination live |
| 7 | **FE-03** auth + Home + Statement ∥ **BE-07** platform upgrade | FE ∥ BE | — | login works against Okta/mock; Home + Statement e2e ∥ suite green on 17→21→25 |
| 8 | **FE-04** operations (Pix, Deposit, Recharge, Withdraw) | FE | — | four flows complete against compose stack |
| 9 | **FE-05** final validation ∥ **BE-08** cleanup | FE ∥ BE | release sign-off | e2e + axe green; no PII in logs; memory updated |
| 10 | **FINAL-01** final test round | both | all specs Done | `docs/test-report.md` with evidence; CI green on `main` |
Rules: a row starts only when the previous rows it depends on are Done (dependency list in `specs/README.md`). `∥` means run in parallel on separate branches/worktrees **only if** they do not touch the same files. Backend specs are never parallel with each other.

## 3. Conventions
* **Branch/PR:** one branch and one PR per spec; commits per task; PR description = spec link + evidence summary. Never push to `main` directly.
* **Commands (backend)** — from `api-financial-transaction-service/`: `./mvnw -B verify` (all tests), `./mvnw -B -Dtest=<Class> test` (focused). From the repo root: `docker compose up --build` (stack; API on 8088). Integration tests use Testcontainers (Docker required).
* **Commands (frontend)** — from `app-financial-transaction-service/`: `npm ci`, `npm start` (4200), `npm run build`, test runner chosen in FE-02, Playwright for e2e against the compose stack.
* **Evidence line:** `> ✅ YYYY-MM-DD HH:MM ±TZ — result. Verification: command → outcome.` Say whether a test was *run* or only *written*.
* **Blocked:** keep the box unticked, add `⛔` line (cause + what unblocks), continue only independent tasks.
* **Skills:** invoke the named skill before the task it owns and follow its instructions (they are procedures, not hints).

## 4. Per-spec runbooks

### BE-01 Safety net
1. Extend `.github/workflows/api-ci.yml` (already created by MONO-01): add JaCoCo. 2. Add test deps (Testcontainers PostgreSQL, WireMock) and an integration base class (`@SpringBootTest`, PostgreSQL container, WireMock for `api.client.maiabank.url`, EmbeddedKafka). 3. Write characterization tests per rule in `produto.md`; tag known defects `// characterizes F-xx` and assert the **current** (wrong) behaviour. 4. Replace vacuous assertions in `AtmWithdrawalServiceImplTest`.
**Verify:** `./mvnw -B verify` locally + CI green; `git diff -- src/main` empty. **Skills:** `java-modern-engineering`, `java-persistence-performance`.

### BE-02 Foundation
1. Create `V1__baseline.sql` from the entities (compare with Hibernate-generated DDL), add Flyway, `ddl-auto: validate`. 2. Split config into base + `local`/`test`/`prod`, all secrets from env, add `application-local.yaml.example`, untrack credentials, fix `.gitignore`. 3. Restrict `DBService`/`TestConfigDb` to `local`. 4. Pin docker-compose tags, pass env vars. 5. Owner rotates the DB credentials.
**Verify:** start twice on empty PostgreSQL (data persists); `git grep -n -i password` clean; BE-01 suite green. **Skill:** `java-persistence-performance` (flyway-migrations).

### BE-03 Money integrity
1. Add the failing concurrency test first (N parallel debits). 2. Implement atomic conditional debit + single balance rule (`>=`); `@Transactional` on the four operations; `save(TransactionTypeEnum…)`. 3. Deposit: remove swallow-catch. 4. Recharge: ledger `accountId` = id; outbox (`V2`) + relay, payload/topic unchanged. 5. Pix: reserve → commit → MaiaBank → confirm/compensate; Feign timeouts; prove the circuit breaker opens. 6. Flip BE-01 tests tagged F-02/F-03/F-08/F-10.
**Verify:** concurrency + failure-injection tests green on PostgreSQL; Kafka payload identical to BE-01. **Skills:** `java-persistence-performance` (transactions-concurrency), `java-modern-engineering`.

### BE-04 Auth and ownership
1. Human sets up Okta exactly as `auth-okta.md` §Okta configuration; record non-secret ids. 2. Add Spring Security resource server, audience validator, authority converter, method security; `V3` (`external_subject`) + `CurrentUser`; remove `DBService` from the main path. 3. Mock JWKS issuer for tests/local; update tests to send tokens. 4. Scope statement/by-id/withdraw by caller; ledger follows debited account. 5. Re-verify every `[VERIFY]` item.
**Verify:** matrix tests (401/403/404), two-user isolation, manual login against a real Okta dev org. **Skills:** `java-modern-engineering`, `java-persistence-performance`.

### BE-05 Behaviour fixes
1. Tests first (single-day range, null/invalid dates, null bodies). 2. Half-open date range in the chosen zone; validators and CPF fixed; handler extended (parse errors, generic 500 with `{code,message}`). **Verify:** suite green; existing messages unchanged. **Skill:** `java-modern-engineering`.

### BE-06 Contract and API evolution
1. **Preview** every contract change using `openapi-rest-contract` and wait for explicit approval. 2. Migrate to openapi-generator (jakarta); record whether bean validation is now enforced. 3. Apply approved corrections; add `GET /accounts/me`, row `id`, pagination/sort/type, aggregate totals, ISO dates (old deprecated), `Idempotency-Key` (`V4`). 4. Statement-count assertion (2 queries); idempotent-replay test; openapi-diff in CI.
**Verify:** contract validation scripts of the skill, Maven generation, full suite. **Skills:** `openapi-rest-contract`, `java-persistence-performance`.

### BE-07 Platform upgrade
Three separate PRs/commits sets: (a) Java 17 build hygiene + Boot 3.5.x + Lombok; (b) Java 21; (c) Java 25 + Docker images. Before (a): save `dependency:tree` and Actuator/Prometheus output as baseline. After (c): `jdeprscan --release 25`, compose smoke. **Verify:** full suite green after each step; metrics unchanged. **Skill:** `java-modern-engineering` (version matrix, migration).

### BE-08 Cleanup and hardening
`Money` + `Account` invariants, dead-code removal, ArchUnit, log masking, correlation id, management port, CI scans. **Verify:** no package moves (`git diff --name-status`), no PII in logs, scans green. **Skill:** `java-modern-engineering`.

### FE-01 Design prototype
The prototype is delivered (`prototype/Maia Bank UI prototype/`). Open `Maia Bank.dc.html`, review every screen/state against brief §13, and write down the divergences (prototype vs backend contract, e.g. boleto deposit; prototype vs brief). Output: sign-off + initial divergence list that seeds `docs/prototype-deviations.md`. **Verify:** checklist ticked, written sign-off. **Skills:** `ui-ux-pro-max`, `angular-tailwind-ui-specialist`.

### FE-02 Frontend foundation (project creation)
1. Project name is fixed: `app-financial-transaction-service`, at the repo root; confirm that directory does **not** exist (the scaffold refuses to overwrite). 2. From the repo root: `node ~/.claude/skills/angular-frontend-scaffold/scripts/scaffold.mjs --output . --project-name app-financial-transaction-service`, then `node ~/.claude/skills/angular-frontend-scaffold/scripts/validate.mjs ./app-financial-transaction-service` (build only; do not run `ng serve`/tests during scaffold validation). 3. Add core: runtime `config.json`, `API_BASE_URL`, error + auth interceptors (dev-open until BE-04), layout shell, dev proxy to `localhost:8088`, OpenAPI type generation (from the BE-06 contract once available). 4. Extract tokens from the prototype (palette, Manrope/Sora, radius, spacing, motion), dark default + light, theme toggle, shared components, `/design` route; create `docs/prototype-deviations.md`. 5. Decide test runner and chart library (record decisions in the project's `doc.md`).
**Verify:** `npm run build`; axe on `/design`; contrast report. **Skills:** `angular-frontend-scaffold`, `angular-tailwind-ui-specialist`, `angular-architect`.

### FE-03 Auth, Home, Statement
Prerequisites: BE-04 (auth) and BE-06 (contract with `/accounts/me`, row ids). Use `angular-module-scaffold` per feature (`features/<module>/` with routes, `pages/`, `ui/`, `data-access/`, `doc.md`) driven by the approved OpenAPI operations. Order: auth (`AuthService` wrapper, guard, callback, interceptor, session-expired) → Home → Statement/detail → AI Assistant concept screen (static, "Coming soon"). **Verify:** unit + e2e against mock issuer; manual login with Okta; axe; 320/768/1280. **Skills:** `angular-module-scaffold`, `angular-tailwind-ui-specialist`, `ui-ux-pro-max`.

### FE-04 Operations
Modules in this order: Withdrawal (simplest) → Deposit → Recharge → Pix. Shared result panel first (success / business failure / uncertain outcome). Tests per module: double-click sends one request, exact balance allowed, no POST auto-retry, withdrawal body is `{amount}` only. **Verify:** each flow end-to-end on `docker compose` stack. **Skills:** as FE-03.

### FE-05 Final validation
Playwright e2e for flows F-0…F-9 of the brief, axe inside e2e, responsive checks, demo README with screenshots, update `memory/` to match what was delivered. **Verify:** CI e2e job green.

## 5. Frontend agent autonomy (applies to FE-02…FE-05)
The prototype is the reference, **not a limit**. The agent proposes and applies improvements within the tiers defined in `memory/contexto-tecnico.md` (*Frontend: prototype and agent autonomy*): Tier 1 (refinements inside the same flow) → apply and log in `app-financial-transaction-service/docs/prototype-deviations.md`; Tier 2 (flow/screen/capability/backend changes) → write a proposal and stop for approval. Backend contract and `produto.md` rules always win over the prototype.

## 6. Parallelism and conflicts
Frontend and backend run in separate repos/folders and can proceed together once their contract dependencies are Done. Inside the backend never run two specs at once. If the backend contract changes after a FE module started, stop the module, regenerate types, adapt, re-test.

## 7. Done for the whole programme
All 13 specs Done · CI green (backend + e2e) · app runs with `docker compose up` and the Angular app against it · both pipelines green independently · `docs/prototype-deviations.md` reviewed · `memory/` matches reality · no open "Stop and ask" items.

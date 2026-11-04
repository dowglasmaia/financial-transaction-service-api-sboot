# Spec Execution Flow

How to execute the specs, in what order, and how to verify each one. Goal: **fix the backend** and **build the complete Angular frontend**, delivered from now on as **vertical slices** (D-31): one slice = one branch = one PR that carries backend + frontend + e2e for one user-visible capability. Protocol for ticking tasks and writing evidence: `shared/como-executar.md`. Base context for every run: `memory/produto.md` + `memory/contexto-tecnico.md`.

> **What changed (2026-11-03):** the visual reference is now **`Maia Bank v2.dc.html`** (D-32), and the remaining work is organized as slices `SL-00…SL-06` followed by the backend-only `BE-07`/`BE-08`, then `FINAL-01` (which absorbs the former `FE-05`; deposit and recharge are one slice since 2026-11-04). `BE-05`, `BE-06`, `FE-03` and `FE-04` are superseded (mapping in `specs/README.md`). Specs `MONO-01`, `BE-01…BE-04`, `FE-02` are Done and unchanged.

## 0. Repository layout
Monorepo: `api-financial-transaction-service/` (backend), `app-financial-transaction-service/` (frontend, created by FE-02), `prototype/` (UI reference), `.spec/`, `.github/workflows/` with **two isolated pipelines** (`api-ci.yml`, `app-ci.yml`, path-filtered). Each spec runs inside its own app folder and must keep its pipeline green without touching the other app.

## 1. The loop (same for every spec)
1. **Gate check** — all dependencies are Done; the spec's human actions are completed; spec status set to *Approved* by the owner (a Draft is never executed).
2. **Prepare** — `git checkout main && git pull origin main`, then new branch `spec/<id>-<slug>` (if main lacks a dependency spec, stop and ask); read memory + the spec; run the test suite once to record the baseline.
3. **Test first** — for each behaviour change write/adjust the failing test (characterization → flip), watch it fail.
4. **Implement the smallest change** for one task; commit (`<id>: <task>`); run the suite.
5. **Verify + evidence** — run the spec's checks; tick the task; add the evidence line under it.
6. **Stop at gates** — any contract/behaviour/scope/dependency change not in the spec → stop and ask.
7. **Close** — all acceptance boxes ticked with proof, fill *Result*, set status in `specs/README.md`, **commit on the spec branch and push it (the agent does not open the PR or merge)**. The owner opens the PR, checks CI, merges; the spec is *Done* after the merge with green CI.
8. **Sync memory** — if reality diverged from `memory/`, update it in the same PR.

Ready-made prompts per spec: `prompts.md`. **Every spec has a *Minimal validation* block; a spec cannot be closed until it passes (and unexecuted checks are reported as not executed).** After all specs are Done, run `FINAL-01` (final test round).

Generic prompt (the per-spec versions in `prompts.md` add the extras):
> Execute SPEC-<ID> (`.spec/specs/<file>`). Read `.spec/memory/produto.md` and `.spec/memory/contexto-tecnico.md` first, then follow `.spec/spec-execution-flow.md` §4 for this spec. Stay in scope, tests first, tick tasks only with evidence, stop at the "Stop and ask" gates.

## 1a. Backend architecture (D-39)
All backend code follows the **Hexagonal Pragmática** model (`memory/contexto-tecnico.md` D-39…D-43; source of truth: the skill `java-spring-create-module`). `ARCH-01`/`ARCH-02` migrate the existing code (behaviour frozen); from then on every backend task of a slice or spec creates or changes a feature **with the skill (mode `create`)**, keeps `ArchitectureTest` green and runs `scripts/check-architecture.mjs` before closing.

## 1b. The slice loop (SL-00…SL-06)
A slice is one branch `slice/<nn>-<slug>` created from a fresh `main`, one PR, and both pipelines (`api-ci`, `app-ci`) must be green on it. Inside a slice the order is fixed so the parts that connect never drift:

1. **Gate** — dependencies Done, the spec's *Human action before* completed, spec *Approved*.
2. **Prepare** — `git checkout main && git pull origin main`, new branch, run the baseline (`./mvnw -B verify`, `npm test`), start the stack (below) and record what is green today.
3. **Contract first** — if the slice changes the API: preview with `openapi-rest-contract` → **explicit approval** → apply to the OpenAPI file. Nothing is generated or coded before the approval.
4. **Backend** — failing tests first (show them red), smallest change, `./mvnw -B verify` green, **created/changed with the skill `java-spring-create-module` (domain/application/infra placement, `ArchitectureTest` green, checker 0 violations)**. One commit per task: `SL-NN: <task>`.
5. **Types** — regenerate the frontend API types from the contract (`npm run api:types`, created in SL-01); commit the diff.
6. **Frontend** — failing unit tests first (Vitest), implement, `npm run build && npm test` green. Tests mirror the backend rules (feedback only; the server decides).
7. **E2E** — Playwright against the real stack with the **mock OIDC provider** (SL-01 creates it) so an agent can sign in without a password; axe inside the e2e on every new screen; 1280 / 768 / 390 viewports.
8. **Browser proof** — walk the slice by hand in the built-in browser (the agent never types a password; for the real Auth0 login the **owner** signs in and reports). Record exactly what was seen.
9. **Close** — tick acceptance with evidence, fill *Result*, update `specs/README.md`, commit and **push the slice branch** (no PR, no merge — the owner does that). Sync `memory/` and `docs/prototype-deviations.md` in the same branch.

**Local stack for slices** (from the repo root): `cp .env.example .env` (once) → `docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api` (db on `localhost:5433`, Kafka `9092`, Kafdrop `19000`, MaiaBank stub `8089`) → API from the IDE run configuration (`SPRING_PROFILES_ACTIVE=local`, `SEED_OWNER_SUB`, `SEED_SECOND_SUB`) on `8088` → `cd app-financial-transaction-service && npm start` on `4200` (dev proxy `/api → 8088`). `docker compose --profile api up -d --build` runs the API in Docker too; for the browser tests use the overlay: `docker compose -f docker-compose.yml -f docker-compose.e2e.yml --profile api --profile e2e up -d --build` (mock IdP + API trusting it), then `cd app-financial-transaction-service && npm run e2e`.

**Rule of thumb:** if a task needs the other side (a field, a status code, a message), it belongs to the **same slice** — never leave a frontend mock or a backend stub for "later".

## 2. Master order
| # | Spec(s) | Track | Human action before | Exit proof |
|---|---|---|---|---|
| 0 | **MONO-01** monorepo + isolated CI (Done) | both | — | both workflows trigger independently |
| 1 | **BE-01 … BE-04** safety net, foundation, money integrity, auth (Done) | BE | Auth0 Action + 2 test users | 98+ tests green; two-user isolation |
| 2 | **FE-01 / FE-02** prototype review + Angular foundation (Done; look replaced in SL-00) | FE | — | `npm run build` green |
| 3 | **SL-00** visual identity v2 (Done) | FE only | owner sign-off of the look | build + tests green; `/design` matches v2; contrast test green |
| 4 | **SL-01** sign in + `/accounts/me` + shell + Home header | BE+FE+E2E | Auth0 callback/logout URLs for `http://localhost:4200`; run API with seed subs | real browser sign-in for both users; e2e with mock IdP green |
| 5 | **ARCH-01** architecture foundation + `account` pilot (backend only) | BE | approve the class mapping table and the security split | suite green, checker 0 violations with the legacy list, `git diff` of resources empty |
| 6 | **ARCH-02** `transaction` feature + legacy cleanup (backend only) | BE | approve the transaction-side mapping | no legacy package; strict checker 0 violations; Kafka payload, concurrency and failure tests unchanged and green |
| 7 | **SL-02** statement + detail + Home summary (+ contract, dates, errors) | BE+FE+E2E | time zone; OpenAPI preview; 400-vs-422; summary endpoint | single-day statement end to end; contract = behaviour; `openapi-diff` in CI |
| 8 | **SL-03** withdrawal + result panel + `Idempotency-Key` | BE+FE+E2E | idempotency design | replay does not double-debit; double click = one request |
| 9 | **SL-04** deposit (boleto simulation) + mobile recharge — one slice, two pages | BE(tests)+FE+E2E | approve the boleto reconciliation | balance +amount once, `DEPOSIT` row; recharge "requested" copy, Kafka payload identical |
| 10 | **SL-06** Pix | BE+FE+E2E | MaiaBank contract only if the payload changes | success/failure/uncertain paths; validators agree client/server |
| 11 | **BE-07** platform upgrade (backend-only) | BE | Boot version, JDK image | suite green on 17→21→25 |
| 12 | **BE-08** cleanup and hardening (backend-only) | BE | rounding rule | no PII in logs; strict checker and `ArchitectureTest` green |
| 13 | **FINAL-01** final test round (includes the former FE-05: e2e matrix, responsive audit, demo README) | both | all specs Done | `docs/test-report.md` with evidence; CI green on `main` |
Rules: a row starts only when the rows it depends on are Done (dependency list in `specs/README.md`). **Slices are strictly sequential.** There is no allowed overlap between specs.

## 3. Conventions
* **Branch/PR:** one branch and one PR per spec **or slice**: `spec/<id>-<slug>` for foundation/backend-only specs, `slice/<nn>-<slug>` for slices; commits per task (`<ID>: <task>`); the **owner opens the PRs manually** (PR description = spec link + evidence summary) and merges before the next spec starts. Agents push only their own branch (after committing) and never commit to or push `main`.
* **Commands (backend)** — from `api-financial-transaction-service/`: `./mvnw -B verify` (all tests), `./mvnw -B -Dtest=<Class> test` (focused). Infra from the repo root: `docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api`; the API runs from the IDE (profile `local`, port 8088) or, from SL-01, `docker compose --profile api up`. Integration tests use Testcontainers (Docker required). Build JDK: 21 (see BE-07 note).
* **Commands (frontend)** — from `app-financial-transaction-service/`: `npm ci`, `npm start` (4200), `npm run build`, `npm test` (Vitest), `npm run api:types` (SL-01), Playwright e2e (SL-01) against the compose stack with `--profile e2e` (mock IdP).
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
1. Human finishes Auth0 as `auth-auth0.md` §Auth0 configuration (Action + two users); record non-secret ids and each user's `sub`. 2. Add Spring Security resource server, audience validator, role-claim converter, method security; `V3` (`external_subject`) + `CurrentUser`; remove `DBService` from the main path. 3. Mock JWKS issuer for tests/local; update tests to send tokens. 4. Scope statement/by-id/withdraw by caller; ledger follows debited account. 5. Re-verify every `[VERIFY]` item.
**Verify:** matrix tests (401/403/404), two-user isolation, manual login against the real Auth0 tenant. **Skills:** `java-modern-engineering`, `java-persistence-performance`.

### BE-05 / BE-06 — superseded
Delivered through the slices: `/accounts/me` (SL-01); generator migration, contract corrections, dates, handler, statement evolution, openapi-diff (SL-02); `Idempotency-Key` + `V4` (SL-03); per-operation validators (SL-03…SL-06). Old text kept in `specs/be-05-behaviour-fixes.md` and `specs/be-06-api-contract-and-evolution.md` as the scope catalogue.

### ARCH-01 Architecture foundation + `account` pilot (backend only)
Skill `java-spring-create-module`, mode `migrate`. 1. Baseline suite green, record the count. 2. Inventory + class mapping table for all 50 classes → **owner approves**. 3. Foundation: ArchUnit dependency, `ArchitectureTest` with the legacy list, `shared` kit (`GlobalExceptionHandler`). 4. Migrate `account` (domain → persistence → service → security split → seed), tests first for the rules, `git mv` for moves. 5. Checker with `--legacy` → 0 violations. **Verify:** `mvn verify`, `git diff main -- src/main/resources` empty, compose smoke. **Skills:** `java-spring-create-module`, `java-persistence-performance`.

### ARCH-02 `transaction` feature + legacy cleanup (backend only)
Same skill and rules. 1. Confirm the transaction-side mapping (which use case classes, one per operation; cross-feature relations; exception names). 2. Domain (`Transaction`) + ports in `application.port` → persistence adapter → use cases → MaiaBank client + outbox/Kafka behind ports → `api` (generated `TransactionsApi` stays in `transaction.api`). 3. Exceptions to `shared.error` (own commit). 4. Delete empty legacy packages, strict checker + ArchUnit with an empty legacy list. **Verify:** unchanged Kafka payload, concurrency, failure-injection and characterization tests; compose smoke; code map rewritten in memory.

### SL-00 Visual identity v2 (frontend only)
1. (Optional, human) screenshots of v2. 2. Review v2 against brief §13 and write the divergence list. 3. Contrast test red → tokens (dark + derived light). 4. Restyle shared components, add segmented/stepper/dialog/toast/icon tile. 5. Shell (rail, bottom nav + More, top bar, demo chip, footer). 6. `/design`. **Verify:** build + tests, v1-colors grep empty, screenshots at 3 widths in 2 themes, owner sign-off. **Skills:** `angular-tailwind-ui-specialist`, `ui-ux-pro-max`.

### SL-01 Sign in and account
1. Human: Auth0 callback/logout URLs for `http://localhost:4200`, Action + users, API seed subs. 2. Contract preview for `GET /accounts/me`. 3. Backend red → green. 4. Library check (`@auth0/auth0-angular` + Angular 21 zoneless), `core/auth`, guard, interceptor, `api:types`. 5. Landing, sign-in, Home header, account menu, dialogs, 403/404. 6. Compose `api` and `e2e` profiles, Playwright + mock IdP. 7. Human: real Auth0 sign-in for both users. **Verify:** `./mvnw verify`, `npm test/build`, e2e, manual sign-in (closes BE-04). **Skills:** `openapi-rest-contract`, `java-modern-engineering`, `angular-module-scaffold`, `auth-oidc-config`.

### SL-02 Statement, detail, Home summary
1. Human: time zone, 400-vs-422, summary endpoint. 2. **Contract preview for all operations** → approval. 3. Backend tests red: single day, boundaries, bad dates, ids, pagination, totals, 2 queries. 4. Generator migration (record the bean-validation answer), corrections, handler, statement evolution (+ summary), `openapi-diff` in CI. 5. Types → Statement, Detail, Home summary with chart + table fallback. 6. E2E incl. cross-customer. **Verify:** suite, contract scripts, e2e + axe. **Skills:** `openapi-rest-contract`, `java-persistence-performance`, `angular-module-scaffold`.

### SL-03 Withdrawal + result panel + idempotency
1. Human: approve the idempotency design. 2. Contract preview (header). 3. Backend: concurrency/replay tests red → `V4` + component in `shared.idempotency`, plus the null/blank body checks of all four POSTs (once). 4. Frontend: operation shell + result panel (success/business/uncertain) + amount presets + review dialog → Withdraw. 5. E2E incl. forced network failure and replay. **Verify:** replay does not double-debit; double click = one request; no POST auto-retry.

### SL-04 Deposit + Recharge (one slice) · SL-06 Pix
Same loop. SL-04 has two pages in one PR (Deposit first, then Recharge); the backend part is operation-specific tests and small fixes because the body checks came with SL-03. Common checks: balance + new statement row in the browser, three result variants, idempotency replay, axe. Specifics: the boleto simulation calls the real deposit for the caller's own account; recharge copy says "requested" and the Kafka payload stays byte-identical (observe in Kafdrop); SL-06 rules agree client/server and a MaiaBank failure leaves no debit.

### FE-03 / FE-04 — superseded
Auth, shell, Home header → SL-01; Home summary, Statement, Detail → SL-02; Withdrawal + shared result panel → SL-03; Deposit and Recharge → SL-04; Pix → SL-06. Old text kept in `specs/fe-03-auth-home-statement.md` and `specs/fe-04-operations.md`. The module layout rule (`features/<module>/` with `routes`, `pages/`, `ui/`, `data-access/`, `doc.md`, via `angular-module-scaffold`) still applies to every slice.

### FE-05 — folded into FINAL-01
Each slice already ships its own Playwright e2e and axe checks. The rest (flows F-0…F-9 on 1280/768/390 in both themes, a manual 320 px pass, demo README with screenshots, memory sync) is part of `FINAL-01` (items E, F, H, J).

## 5. Frontend agent autonomy (applies to SL-00…SL-06 and FINAL-01)
The prototype (**v2**) is the reference, **not a limit**. The agent proposes and applies improvements within the tiers defined in `memory/contexto-tecnico.md` (*Frontend: prototype and agent autonomy*): Tier 1 (refinements inside the same flow) → apply and log in `app-financial-transaction-service/docs/prototype-deviations.md`; Tier 2 (flow/screen/capability/backend changes) → write a proposal and stop for approval. Backend contract and `produto.md` rules always win over the prototype.

## 6. Parallelism and conflicts
`ARCH-01` and `ARCH-02` are backend-only and strictly sequential; `ARCH-01` may run in parallel with `SL-00` (different folders). Backend and frontend of a slice move **together on one branch**, in the fixed order of §1b. Never run two slices (or a slice and a backend spec) at once: they share backend services and the app shell. There is no allowed overlap. If the contract changes after the frontend started, stop, regenerate types, adapt, re-test; if a slice needs a contract change that was not previewed, that is a "Stop and ask".

## 7. Done for the whole programme
All specs Done (foundation, ARCH-01/02, SL-00…SL-06 (SL-05 merged into SL-04), BE-07, BE-08, FINAL-01) · CI green (backend + e2e) · app runs with `docker compose up` and the Angular app against it · both pipelines green independently · `docs/prototype-deviations.md` reviewed · `memory/` matches reality · no open "Stop and ask" items.

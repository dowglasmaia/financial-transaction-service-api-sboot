# SPEC-SL-02 — Statement, detail and Home summary (contract evolution + date fixes)

**Status:** Implemented on the branch, awaiting owner review (revised 2026-11-04) · **Depends on:** ARCH-02 (merged, PR #18) and SL-01 · **Parallel with:** none
**Branch:** `slice/02-statement-and-detail` · **Track:** Backend + Frontend + E2E
**Absorbs:** BE-05 (dates, handler), BE-06 (generator migration, contract corrections, statement evolution, openapi-diff), FE-03 (Home summary, Statement, Detail).
**Read first:** `../memory/produto.md` (rules 8, 9; *Decided changes*), `../memory/contexto-tecnico.md` (F-07, F-12), `../spec-execution-flow.md` §1b, v2 reference (Home summary + chart + recent activity, Statement, Detail). Protocol: `../shared/como-executar.md`.

## Objective
Make money history readable and trustworthy end to end: a statement for any day/range returns the right rows, the contract tells the truth about every status code, the UI shows server-computed totals, filters, sorting and pagination, a transaction detail by id, and a Home summary — all for the signed-in customer only.

## Human action before
1. Choose the **time zone for day boundaries** (default proposal: `America/Sao_Paulo`).
2. Be ready to approve the **OpenAPI preview** (it covers every operation, not only the statement).
3. Decide the **400 vs 422** rule for field validation (default: keep 422 everywhere, documented).
4. Approve (or refuse) the proposed **summary endpoint** (below). If refused, Home shows totals and recent rows from the statement endpoint only, and the chart is dropped.

## Scope
**Backend**
- Generator migration swagger-codegen → **openapi-generator (jakarta)**, keeping the generated package names and controller signatures stable; **prove and record whether generated bean validation is now enforced** (the old `javax` question).
- Contract corrections for **all** operations: real statuses 204/404/422/401/403, enums (types, carriers, key types), `bearerAuth`, server URL, remove unused schemas.
- Statement: half-open range `[start 00:00, end+1d 00:00)` in the chosen zone; input stays `dd/MM/yyyy`; **ISO params `from`/`to` added**, old params kept and marked deprecated; null/invalid/reversed dates → defined 422 (never 500/NPE); each row carries **`id`**; `page`, `size`, `sort` (`date|amount`, `asc|desc`) and `type` filter; response adds pagination metadata (`page`, `size`, `totalElements`) additively; `sumIncoming`/`sumOutgoing`/`balance` computed by **aggregate queries over the whole range** (not the page), statement-count assertion (2 queries, no N+1). **Placement (layout v2, D-50):** `GetStatementUseCase` returns the value object `Statement`, extended with the totals and the pagination metadata; the aggregate and paged queries are new methods of the `TransactionRepository` port (adapter in `transaction.infrastructure.persistence`); the income/outgoing classification, which today lives in `TransactionMapper` and is pinned by `TransactionMapperTest`, moves into the domain (`Statement`) in this slice and the test moves with it; the controller in `transaction.api` only maps `Statement` to the generated DTO.
- `GET /transactions/{id}` unchanged semantics (foreign id → 404) but documented.
- `shared.web.GlobalExceptionHandler` (formerly `HandlerException`): parse errors (bad date/JSON), generic `Exception` → 500 `{code,message}` without internals, unused `HttpStatus` field resolved, `@EnableWebMvc` assessed; all existing messages unchanged.
- **Proposed (needs approval): `GET /transactions/summary?from&to`** → `{sumIncoming, sumOutgoing, balance, series:[{date, incoming, outgoing}]}` (daily buckets in the zone) so Home never aggregates in the browser; new use case `GetTransactionsSummaryUseCase`.
- `openapi-diff` check in `api-ci` (fails on breaking changes to the contract).
- Flyway: only if a measured query needs it (new index = Stop and ask).

**Frontend**
- Regenerated API types; `features/statement` and `features/home` `data-access` (all HTTP here).
- **Statement page:** period chips (7 / 30 / 90 days + custom range) → `from/to`; type chips (All / Deposit / Pix / Recharge / Withdraw) → `type`; sort by date or amount; "Showing X of Y"; summary tiles (Incoming, Outgoing, Current balance) from the backend; list on mobile, table on desktop; filters live in query params (shareable, back button works); loading / empty ("No transactions in this period" + change period) / error (request id + retry).
- **Detail page:** by id; label, signed amount, date/time, status "Registered", shortened id with copy action (toast "Transaction id copied"); unknown/foreign id → "Transaction not found" state (404).
- **Home summary:** period chips, Incoming/Outgoing/Current balance, hand-made SVG line chart with the **"View as table" fallback**, 5 recent rows ("See all →"), empty state ("No activity yet. Make a deposit to get started."). The UI never computes balance or totals.
- Nav entries "Home" and "Transactions" active.

**Out of scope:** operations (SL-03…06), idempotency (SL-03), CSV export, search.

## Tasks
- [x] HUMAN: time zone, 400-vs-422 rule, summary-endpoint decision — _evidence: America/Sao_Paulo, 422 for fields (400 only for a malformed path id), summary endpoint approved ("Aprovado")_
- [x] CONTRACT: preview **all** contract changes with `openapi-rest-contract`; wait for approval — _evidence: preview approved by the owner before anything was generated (contract 1.1.0, commit afbf8a9)_
- [x] BE tests first (red): single-day range returns that day; boundary rows at 00:00 and 23:59:59 in the zone; null/bad/reversed dates → 422; row `id`; pagination/sort/type; totals independent of page; statement uses 2 queries; foreign id → 404; error JSON for unexpected failures — _evidence: red shown: test-compile failed on the missing StatementPeriod/StatementQuery/StatementTotals/DailyTotal/TransactionsSummary; `StatementEvolutionTest` covers the list incl. 2-query assertion_
- [x] BE: generator migration; record the bean-validation answer — _evidence: commit ecff0a9, 189 tests green; generated bean validation stays OFF on purpose (ValidateBodyRequest messages are the contract)_
- [x] BE: contract corrections applied; handler extended; ISO params + deprecation of the old ones — _evidence: commit afbf8a9 (+400 on GET /transactions/{id} in fb5526e)_
- [x] BE: statement evolution (+ summary endpoint if approved); `./mvnw -B verify`; `openapi-diff` in `api-ci` — _evidence: fb5526e; `./mvnw -B verify` on JDK 21: 220 run, 0 failures, 1 skipped (live tenant); strict checker 0 violations. `openapi-diff` job WRITTEN in api-ci, NOT executed (needs the PR)_
- [x] FE: `api:types` regenerated; unit tests first for query-param mapping, formatters, empty/error states — _evidence: commit 7207f74; specs first (query mapping, api, stores, chart)_
- [x] FE: Statement, Detail, Home summary + chart + table fallback — _evidence: commit 7207f74; `npm test` 220 passed, `ng build` ok_
- [x] E2E: single-day filter shows that day's rows; type chip + sort + pagination; detail by id; user 2 opening user 1's id gets the not-found state; Home totals equal Statement totals for the same period; axe on Statement, Detail, Home — _evidence: `npx playwright test`: 42 passed (15 new = 5 scenarios x 3 viewports, axe no serious violations)_
- [x] Postman: statement requests updated to the new params; expected statuses — _evidence: 6 requests added (SL-02 folder); WRITTEN, not run_
- [x] Docs: `docs/prototype-deviations.md` rows for ids, period chips, dropped elements — _evidence: `app-financial-transaction-service/docs/prototype-deviations.md` rows 10-15_

## Acceptance
- [x] A statement for a single day returns that day's rows in the chosen zone — _evidence: StatementEvolutionTest + e2e single day_
- [x] No validation path returns 500/NPE; every existing message and the 204/404/422 contract still hold — _evidence: 422 handler + tests_
- [ ] The OpenAPI file matches real behaviour for every operation/status; `openapi-diff` is in CI
- [x] Frontend renders Home and Statement/Detail from the contract only; totals come from the backend — _evidence: totals from the backend (stores never add up); e2e Home totals == Statement totals_
- [x] Cross-customer check in the browser: user 2 sees none of user 1's rows, ids or totals — _evidence: e2e: user 2 gets the not-found state for user 1's id and sees only their own row_

## Minimal validation (required to close this spec)
- [x] `./mvnw -B verify` green (report test count, coverage, and the statement-count assertion) — _evidence: 220 tests; statement-count assertion = 2 ledger queries_
- [ ] Contract validation scripts of the skill pass and Maven generation succeeds
- [x] `npm ci && npm run build && npm test` green; E2E green on 3 viewports; axe no serious violations — _evidence: build ok, 220 unit tests; E2E 42/42 on 1280/768/390_
- [ ] Manual (owner or built-in browser with the mock IdP): single-day filter, detail by id, Home summary
- [ ] Both pipelines green on the PR

## Stop and ask
- **Size:** this is the largest slice. If it will not fit one reviewable PR, stop and propose splitting the Home summary (chart + summary endpoint) into an optional `SL-02b`.
- Time zone; 400 vs 422; contract preview approval; summary endpoint; any new table/index; generator changes that alter generated package names

## Skills
openapi-rest-contract, java-persistence-performance, java-modern-engineering, angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max

## Result
**Result (2026-11-04):** statement evolution, summary endpoint, detail by id and Home summary delivered on `slice/02-statement-and-detail`; also fixed dates serialised as epoch numbers/arrays (contract says ISO strings) and added a Dockerfile + compose service for the web app.

**Checks executed:** `./mvnw -B verify` (JDK 21) 220/0 failures/1 skipped; skill architecture checker `--strict` 0 violations (1 warning W04: 16 classes in `transaction.domain`); `npm test` 220; `ng build`; Playwright 42/42 on three viewports with axe; web image built and smoke-tested (SPA deep link, config.json, /api proxy).
**Written, not executed:** `openapi-diff` job (needs a PR), the 6 Postman requests, the compose `web` profile with the e2e config mount.
**Pending for the owner:** manual check in a browser; PR and both pipelines; skill `inspect-contract` only reads OpenAPI 3.1 (this contract is 3.0), so it was not used.
**Accepted limits:** the e2e writes ledger rows straight into PostgreSQL (no screen creates money before SL-03); the shell default `JAVA_HOME` (JDK 25) breaks Mockito, use JDK 21 as the pom targets Java 17; SL-01 manual real-Auth0 test still pending (user 2 403).

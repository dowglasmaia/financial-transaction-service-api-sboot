# SPEC-SL-02 — Statement, detail and Home summary (contract evolution + date fixes)

**Status:** Draft (not authorized; revised 2026-11-04) · **Depends on:** ARCH-02 (merged, PR #18) and SL-01 · **Parallel with:** none
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
- [ ] HUMAN: time zone, 400-vs-422 rule, summary-endpoint decision
- [ ] CONTRACT: preview **all** contract changes with `openapi-rest-contract`; wait for approval
- [ ] BE tests first (red): single-day range returns that day; boundary rows at 00:00 and 23:59:59 in the zone; null/bad/reversed dates → 422; row `id`; pagination/sort/type; totals independent of page; statement uses 2 queries; foreign id → 404; error JSON for unexpected failures
- [ ] BE: generator migration; record the bean-validation answer
- [ ] BE: contract corrections applied; handler extended; ISO params + deprecation of the old ones
- [ ] BE: statement evolution (+ summary endpoint if approved); `./mvnw -B verify`; `openapi-diff` in `api-ci`
- [ ] FE: `api:types` regenerated; unit tests first for query-param mapping, formatters, empty/error states
- [ ] FE: Statement, Detail, Home summary + chart + table fallback
- [ ] E2E: single-day filter shows that day's rows; type chip + sort + pagination; detail by id; user 2 opening user 1's id gets the not-found state; Home totals equal Statement totals for the same period; axe on Statement, Detail, Home
- [ ] Postman: statement requests updated to the new params; expected statuses
- [ ] Docs: `docs/prototype-deviations.md` rows for ids, period chips, dropped elements

## Acceptance
- [ ] A statement for a single day returns that day's rows in the chosen zone
- [ ] No validation path returns 500/NPE; every existing message and the 204/404/422 contract still hold
- [ ] The OpenAPI file matches real behaviour for every operation/status; `openapi-diff` is in CI
- [ ] Frontend renders Home and Statement/Detail from the contract only; totals come from the backend
- [ ] Cross-customer check in the browser: user 2 sees none of user 1's rows, ids or totals

## Minimal validation (required to close this spec)
- [ ] `./mvnw -B verify` green (report test count, coverage, and the statement-count assertion)
- [ ] Contract validation scripts of the skill pass and Maven generation succeeds
- [ ] `npm ci && npm run build && npm test` green; E2E green on 3 viewports; axe no serious violations
- [ ] Manual (owner or built-in browser with the mock IdP): single-day filter, detail by id, Home summary
- [ ] Both pipelines green on the PR

## Stop and ask
- **Size:** this is the largest slice. If it will not fit one reviewable PR, stop and propose splitting the Home summary (chart + summary endpoint) into an optional `SL-02b`.
- Time zone; 400 vs 422; contract preview approval; summary endpoint; any new table/index; generator changes that alter generated package names

## Skills
openapi-rest-contract, java-persistence-performance, java-modern-engineering, angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max

## Result
_Filled on completion: result, checks run, accepted limitations._

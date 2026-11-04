# Specs — index

> **Ready-to-paste prompts: `../prompts.md`. Every spec has a *Minimal validation* block that must pass before it can be closed.**
>
> **How to execute and in what order: `../spec-execution-flow.md`** (authoritative). This file is only the index and dependency list.

SDD: nothing is implemented without an approved spec. A spec starts when its dependencies are **Done**. Protocol: `../shared/como-executar.md`. Base context: `../memory/`.

## Two kinds of work
1. **Foundation specs (done or backend-only)** — `MONO-01`, `BE-01…BE-04`, `FE-01`, `FE-02`, and the closing specs `BE-07`, `BE-08`, `FINAL-01` (which absorbs the former `FE-05`).
2. **Vertical slices (D-31)** — from now on every user-visible capability is delivered as **one slice = one branch = one PR carrying backend + frontend + e2e together**, so the parts that connect always move together and can be tried in the browser at the end of each slice.

`BE-05`, `BE-06`, `FE-03` and `FE-04` are **superseded**: their scope was split across the slices (mapping below). Their files stay as the scope catalogue.

## Order
> **Revised 2026-11-04:** ARCH-01 and ARCH-02 are merged. Deposit and recharge became one slice (SL-04, branch `slice/04-deposit-and-recharge`); the body-shape validation of all four POSTs moved to SL-03; FE-05 was folded into FINAL-01. SL-05 and FE-05 have no prompt any more.

`MONO-01 · BE-01…BE-04 · FE-01 · FE-02` (done) → **`SL-00`** → **`SL-01`** → **`ARCH-01 → ARCH-02`** → **`SL-02 → SL-03 → SL-04 (deposit + recharge) → SL-06`** → `BE-07` → `BE-08` → `FINAL-01` (includes the former FE-05)

Slices are strictly sequential (they share backend services and the app shell). **`ARCH-01` and `ARCH-02` (backend architecture migration, D-39) run right after `SL-01`** (owner decision 2026-11-03, D-44: finish the login flow first) and **before `SL-02`**, so every later endpoint is created in its final place with the skill `java-spring-create-module`. `SL-01` is the one slice built on the current layered backend.

| Spec | Title | Depends on | Branch |
|---|---|---|---|
| [MONO-01](mono-01-monorepo.md) | Monorepo restructure + isolated CI | — | `spec/mono-01-monorepo` |
| [BE-01](be-01-safety-net.md) | Safety net: CI + characterization tests | — | `spec/be-01-safety-net` |
| [BE-02](be-02-foundation-config-db.md) | Foundation: profiles, secrets, Flyway | BE-01 | `spec/be-02-foundation-config-db` |
| [BE-03](be-03-money-integrity.md) | Money integrity: transactions, atomic balance, Pix, recharge | BE-02 | `spec/be-03-money-integrity` |
| [BE-04](be-04-auth-and-ownership.md) | Auth0 authentication, CurrentUser, ownership | BE-03 | `spec/be-04-auth-and-ownership` |
| [FE-01](fe-01-design-prototype.md) | UI/UX prototype review (v1; **superseded by SL-00 for v2**) | — | `spec/fe-01-design-prototype` |
| [FE-02](fe-02-foundation.md) | Angular foundation: scaffold, core, design system (v1 look) | BE-02, FE-01 | `spec/fe-02-foundation` |
| [ARCH-01](arch-01-hexagonal-pilot.md) | **Hexagonal Pragmática: foundation + `account` pilot** (backend only, behaviour frozen) | BE-04, SL-01, skill updated | `spec/arch-01-hexagonal-pilot` |
| [ARCH-02](arch-02-hexagonal-transaction.md) | **Hexagonal Pragmática: `transaction` feature + legacy cleanup** (backend only) | ARCH-01 | `spec/arch-02-hexagonal-transaction` |
| [SL-00](sl-00-visual-v2.md) | **Visual identity v2** (frontend-only) | FE-02 | `slice/00-visual-v2` |
| [SL-01](sl-01-login-and-account.md) | **Sign in (Auth0) + `/accounts/me` + shell + Home header** | SL-00, BE-04 | `slice/01-login-and-account` |
| [SL-02](sl-02-statement-and-detail.md) | **Statement, detail, Home summary** (+ contract, dates, errors) | ARCH-02 | `slice/02-statement-and-detail` |
| [SL-03](sl-03-withdrawal.md) | **Withdrawal** + result panel + `Idempotency-Key` | SL-02 | `slice/03-withdrawal` |
| [SL-04](sl-04-deposit-and-recharge.md) | **Deposit + Mobile recharge** (boleto simulation over the real deposit; "requested" recharge) — one slice, two pages | SL-03 | `slice/04-deposit-and-recharge` |
| ~~[SL-05](sl-05-recharge.md)~~ | Merged into SL-04 2026-11-04 | — | — |
| [SL-06](sl-06-pix.md) | **Pix** (3 steps, MaiaBank) | SL-04 | `slice/06-pix` |
| [BE-07](be-07-platform-upgrade.md) | Platform upgrade: Boot 3.5.x, Java 21, Java 25 (backend-only) | SL-06 | `spec/be-07-platform-upgrade` |
| [BE-08](be-08-cleanup-hardening.md) | Cleanup and portfolio-grade hardening (backend-only) | BE-07 | `spec/be-08-cleanup-hardening` |
| ~~[FE-05](fe-05-final-validation.md)~~ | Folded into FINAL-01 2026-11-04 | — | — |
| [E2E-01](e2e-01-cypress-suite.md) | **Cypress suite** (one spec: flows, API matrix, manual-round checklist) | BE-07, BE-08 | `spec/e2e-01-cypress-suite` |
| [FINAL-01](final-01-test-round.md) | Final test round (after all specs) | all others | `spec/final-01-test-round` |
| ~~[BE-05](be-05-behaviour-fixes.md)~~ | Superseded | — | — |
| ~~[BE-06](be-06-api-contract-and-evolution.md)~~ | Superseded | — | — |
| ~~[FE-03](fe-03-auth-home-statement.md)~~ | Superseded | — | — |
| ~~[FE-04](fe-04-operations.md)~~ | Superseded | — | — |

## Where the superseded scope went
| Old item | Delivered in |
|---|---|
| BE-05 dates (half-open range, null/invalid), `HandlerException` (parse errors, 500 JSON) | SL-02 |
| BE-05 null/blank body checks of the four POSTs | SL-03 (all four, once) |
| BE-05 operation rules | SL-04 (deposit, recharge phone/carrier), SL-06 (Pix: CPF, e-mail, phone) |
| BE-06 `GET /accounts/me` | SL-01 |
| BE-06 generator migration, contract corrections, row `id`, pagination/sort/type, totals, ISO dates, openapi-diff | SL-02 |
| BE-06 `Idempotency-Key` (`V4`) | SL-03 |
| FE-03 auth, shell, Home header | SL-01 |
| FE-03 Home summary, Statement, Detail | SL-02 |
| FE-03 AI Assistant concept card | Dropped (v2 has none; D-35) |
| FE-04 Withdrawal + shared result panel | SL-03 |
| FE-04 Deposit + Recharge / Pix | SL-04 / SL-06 |
| FE-05 e2e matrix, responsive audit, demo README | FINAL-01 (items E, F, J) |
| BE-04's pending manual Auth0 sign-in | SL-01 (human task) |

## Why this order
1. Foundation first (BE-01…04, FE-02): tests, schema, money integrity and auth must exist before any screen is real.
2. **SL-00 before SL-01:** the visual identity changed (v2), so tokens, shared components and the shell are replaced once, before any screen is written.
3. **ARCH-01/02 before the first backend slice:** SL-01…SL-06 add and change backend code; doing it on the old layered packages and migrating later would mean writing it twice. The migration is behaviour-frozen and guarded by the 100+ existing tests; `account` goes first as the pilot because it is the smallest feature.
4. **SL-01 first slice:** the smallest end-to-end chain (browser → Auth0 → API → DB) that the owner can try; everything later relies on it.
5. **SL-02 before the operations:** you can only judge an operation by what it does to the balance and the statement.
6. **SL-03 before SL-04 and SL-06:** it creates the shared result panel, amount presets, review dialog and the idempotency protection the other flows reuse, and hardens the body validation of all four POSTs once; withdrawal is the simplest body.
6b. **Deposit and recharge together (2026-11-04):** after SL-03 both are almost only frontend and share every component, so they are one slice with two pages (Deposit first).
7. **Pix last:** it has the external call, the most validators and the uncertain-outcome path.
8. **BE-07 after the slices:** functional work first, platform upgrades last, so regressions are attributable.

## Human actions required
Approve the class mapping table and the security split (ARCH-01) and the transaction-side mapping (ARCH-02) · Auth0 portal for SPA callback URLs + two test users (SL-01) · approve OpenAPI previews (SL-01, SL-02, SL-03) · time zone and 400-vs-422 (SL-02) · idempotency design (SL-03) · boleto reconciliation (SL-04) · MaiaBank contract only if the Pix payload must change (SL-06) · owner sign-off of the v2 look (SL-00) · rotate old DB credential `maia/maiapw` (BE-02 carry-over) · rounding rule (BE-08) · Boot version / JDK image (BE-07).

## Status
| Spec | Status |
|---|---|
| MONO-01 | Done (merged in PR #1, 2026-11-02) |
| BE-01 | Done (PR #1 merged 2026-11-02; api-ci green on 062cf0b) |
| BE-02 | Done (PR #3 merged 2026-11-02; api-ci green on 567c8ab). Owner still owes the rotation of the old DB credential |
| BE-03 | Done (PR #7 merged 2026-11-02) |
| BE-04 | Merged (PR #9); automated two-user scenario added; **manual Auth0 browser sign-in moves to SL-01** |
| FE-01 | Superseded by SL-00 (v2 sign-off happens there) |
| FE-02 | Done (PR #5 merged 2026-11-02; app-ci green on 48230d5). Look replaced by SL-00. Carried over: axe run → SL-01/FINAL-01, OpenAPI type generation → SL-01 |
| ARCH-01 | Done (PR #17 merged; 156 tests, checker 0 violations, layout v2 / D-50) |
| ARCH-02 | Done (PR #18 merged; 184 tests, strict checker 0 violations, no legacy package) |
| SL-00 | Done (PR #13 merged); owner signed off the look 2026-11-03 (record: commit `7a8a37e` on `slice/00-visual-v2`, not merged) |
| SL-01 | Merged (PR #16), verified with the mock IdP (116 backend tests, 174 unit tests, 21 e2e). **Open (owner):** real Auth0 sign-in for both users + the portal URLs, then the SL-01 continuation prompt closes it. Tokens stay in memory (owner decision 2026-11-03) |
| SL-02 | Merged (PR #22); contract 1.1.0, statement/detail/summary, openapi-diff job |
| SL-03 | Merged (PR #23) |
| SL-04 | Merged (PR #24) |
| SL-06 | Merged (PR #25) |
| SL-05 | Merged into SL-04 (2026-11-04) |
| BE-07 | Implemented on `spec/be-07-08-backend-close` (Boot 3.5.16, Java 25, 410 tests with BE-08); awaiting the owner's PR, review and green pipelines |
| BE-08 | Implemented on `spec/be-07-08-backend-close` together with BE-07; awaiting the owner's PR, review and green pipelines |
| E2E-01 | Draft (written 2026-11-04): one Cypress suite next to Playwright; needs the owner's approval to implement |
| FE-05 | Folded into FINAL-01 (2026-11-04) |
| FINAL-01 | Draft |
| BE-05, BE-06, FE-03, FE-04 | Superseded by the slices |

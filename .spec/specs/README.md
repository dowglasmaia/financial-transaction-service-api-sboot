# Specs — index

> **Ready-to-paste prompts: `../prompts.md`. Every spec has a *Minimal validation* block that must pass before it can be closed.**
>
> **How to execute and in what order: `../spec-execution-flow.md`** (authoritative). This file is only the index and dependency list.

SDD: nothing is implemented without an approved spec. A spec starts when its dependencies are **Done**. Protocol: `../shared/como-executar.md`. Base context: `../memory/`.

## Two kinds of work
1. **Foundation specs (done or backend-only)** — `MONO-01`, `BE-01…BE-04`, `FE-01`, `FE-02`, and the closing backend specs `BE-07`, `BE-08`, `FE-05`, `FINAL-01`.
2. **Vertical slices (D-31)** — from now on every user-visible capability is delivered as **one slice = one branch = one PR carrying backend + frontend + e2e together**, so the parts that connect always move together and can be tried in the browser at the end of each slice.

`BE-05`, `BE-06`, `FE-03` and `FE-04` are **superseded**: their scope was split across the slices (mapping below). Their files stay as the scope catalogue.

## Order
`MONO-01 · BE-01…BE-04 · FE-01 · FE-02` (done) → **`SL-00 ∥ ARCH-01`** → **`ARCH-02`** → **`SL-01 → SL-02 → SL-03 → SL-04 → SL-05 → SL-06`** → `BE-07` → `BE-08 ∥ FE-05` → `FINAL-01`

Slices are strictly sequential (they share backend services and the app shell). **`ARCH-01` and `ARCH-02` (backend architecture migration, D-39) come before the first backend slice** so every new endpoint is created in its final place with the skill `java-spring-create-module`; `ARCH-01` may run in parallel with `SL-00` because they touch different folders.

| Spec | Title | Depends on | Branch |
|---|---|---|---|
| [MONO-01](mono-01-monorepo.md) | Monorepo restructure + isolated CI | — | `spec/mono-01-monorepo` |
| [BE-01](be-01-safety-net.md) | Safety net: CI + characterization tests | — | `spec/be-01-safety-net` |
| [BE-02](be-02-foundation-config-db.md) | Foundation: profiles, secrets, Flyway | BE-01 | `spec/be-02-foundation-config-db` |
| [BE-03](be-03-money-integrity.md) | Money integrity: transactions, atomic balance, Pix, recharge | BE-02 | `spec/be-03-money-integrity` |
| [BE-04](be-04-auth-and-ownership.md) | Auth0 authentication, CurrentUser, ownership | BE-03 | `spec/be-04-auth-and-ownership` |
| [FE-01](fe-01-design-prototype.md) | UI/UX prototype review (v1; **superseded by SL-00 for v2**) | — | `spec/fe-01-design-prototype` |
| [FE-02](fe-02-foundation.md) | Angular foundation: scaffold, core, design system (v1 look) | BE-02, FE-01 | `spec/fe-02-foundation` |
| [ARCH-01](arch-01-hexagonal-pilot.md) | **Hexagonal Pragmática: foundation + `account` pilot** (backend only, behaviour frozen) | BE-04, skill updated | `spec/arch-01-hexagonal-pilot` |
| [ARCH-02](arch-02-hexagonal-transaction.md) | **Hexagonal Pragmática: `transaction` feature + legacy cleanup** (backend only) | ARCH-01 | `spec/arch-02-hexagonal-transaction` |
| [SL-00](sl-00-visual-v2.md) | **Visual identity v2** (frontend-only) | FE-02 | `slice/00-visual-v2` |
| [SL-01](sl-01-login-and-account.md) | **Sign in (Auth0) + `/accounts/me` + shell + Home header** | SL-00, ARCH-02, BE-04 | `slice/01-login-and-account` |
| [SL-02](sl-02-statement-and-detail.md) | **Statement, detail, Home summary** (+ contract, dates, errors) | SL-01 | `slice/02-statement-and-detail` |
| [SL-03](sl-03-withdrawal.md) | **Withdrawal** + result panel + `Idempotency-Key` | SL-02 | `slice/03-withdrawal` |
| [SL-04](sl-04-deposit.md) | **Deposit** (boleto simulation over the real deposit) | SL-03 | `slice/04-deposit` |
| [SL-05](sl-05-recharge.md) | **Mobile recharge** | SL-04 | `slice/05-recharge` |
| [SL-06](sl-06-pix.md) | **Pix** (3 steps, MaiaBank) | SL-05 | `slice/06-pix` |
| [BE-07](be-07-platform-upgrade.md) | Platform upgrade: Boot 3.5.x, Java 21, Java 25 (backend-only) | SL-06 | `spec/be-07-platform-upgrade` |
| [BE-08](be-08-cleanup-hardening.md) | Cleanup and portfolio-grade hardening (backend-only) | BE-07 | `spec/be-08-cleanup-hardening` |
| [FE-05](fe-05-final-validation.md) | End-to-end, accessibility and final review | SL-06, BE-07 | `spec/fe-05-final-validation` |
| [FINAL-01](final-01-test-round.md) | Final test round (after all specs) | all others | `spec/final-01-test-round` |
| ~~[BE-05](be-05-behaviour-fixes.md)~~ | Superseded | — | — |
| ~~[BE-06](be-06-api-contract-and-evolution.md)~~ | Superseded | — | — |
| ~~[FE-03](fe-03-auth-home-statement.md)~~ | Superseded | — | — |
| ~~[FE-04](fe-04-operations.md)~~ | Superseded | — | — |

## Where the superseded scope went
| Old item | Delivered in |
|---|---|
| BE-05 dates (half-open range, null/invalid), `HandlerException` (parse errors, 500 JSON) | SL-02 |
| BE-05 validators / null bodies per operation | SL-03 (withdraw), SL-04 (deposit), SL-05 (recharge, phone), SL-06 (Pix: CPF, e-mail, phone) |
| BE-06 `GET /accounts/me` | SL-01 |
| BE-06 generator migration, contract corrections, row `id`, pagination/sort/type, totals, ISO dates, openapi-diff | SL-02 |
| BE-06 `Idempotency-Key` (`V4`) | SL-03 |
| FE-03 auth, shell, Home header | SL-01 |
| FE-03 Home summary, Statement, Detail | SL-02 |
| FE-03 AI Assistant concept card | Dropped (v2 has none; D-35) |
| FE-04 Withdrawal + shared result panel | SL-03 |
| FE-04 Deposit / Recharge / Pix | SL-04 / SL-05 / SL-06 |
| BE-04's pending manual Auth0 sign-in | SL-01 (human task) |

## Why this order
1. Foundation first (BE-01…04, FE-02): tests, schema, money integrity and auth must exist before any screen is real.
2. **SL-00 before SL-01:** the visual identity changed (v2), so tokens, shared components and the shell are replaced once, before any screen is written.
3. **ARCH-01/02 before the first backend slice:** SL-01…SL-06 add and change backend code; doing it on the old layered packages and migrating later would mean writing it twice. The migration is behaviour-frozen and guarded by the 100+ existing tests; `account` goes first as the pilot because it is the smallest feature.
4. **SL-01 first slice:** the smallest end-to-end chain (browser → Auth0 → API → DB) that the owner can try; everything later relies on it.
5. **SL-02 before the operations:** you can only judge an operation by what it does to the balance and the statement.
6. **SL-03 before SL-04…06:** it creates the shared result panel and the idempotency protection the other three flows reuse; withdrawal is the simplest body.
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
| FE-02 | Done (PR #5 merged 2026-11-02; app-ci green on 48230d5). Look replaced by SL-00. Carried over: axe run → SL-01/FE-05, OpenAPI type generation → SL-01 |
| SL-00 | Implemented on `slice/00-visual-v2` (2026-11-03); owner signed off the look (2026-11-03); **open: `app-ci` on the PR** |
| ARCH-01 | Draft (spec written 2026-11-03; not authorized) |
| ARCH-02 | Draft (spec written 2026-11-03; not authorized) |
| SL-00 … SL-06 | Draft (specs written 2026-11-03; not authorized) |
| SL-00 | Implemented on `slice/00-visual-v2` (2026-11-03); **open: owner sign-off of the look + `app-ci` on the PR** |
| SL-01 … SL-06 | Draft (specs written 2026-11-03; not authorized) |
| BE-07 | Draft (Java 21 build already pre-applied locally, uncommitted; see the spec note) |
| BE-08 | Draft |
| FE-05 | Draft |
| FINAL-01 | Draft |
| BE-05, BE-06, FE-03, FE-04 | Superseded by the slices |

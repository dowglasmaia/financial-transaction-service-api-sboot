# Specs — index

> **Ready-to-paste prompts: `../prompts.md`. Every spec has a *Minimal validation* block that must pass before it can be closed.**
>
> **How to execute and in what order: `../spec-execution-flow.md`** (authoritative). This file is only the index and dependency list.

SDD: nothing is implemented without an approved spec. All are **Draft**. A spec starts when its dependencies are **Done**. Protocol: `../shared/como-executar.md`. Base context: `../memory/`.

## Step 0
`MONO-01` (monorepo + isolated CI) — [mono-01-monorepo.md](mono-01-monorepo.md) — **Done** (PR #1).

## Backend (strictly sequential — they touch the same services)
`BE-01 → BE-02 → BE-03 → BE-04 → BE-05 → BE-06 → BE-07 → BE-08`

## Frontend (runs alongside)
`FE-01 (design, can start now) → FE-02 → FE-03 → FE-04 → FE-05`

## Last
`FINAL-01` — final test round, only after every other spec is Done.

| Spec | Title | Depends on | Parallel with |
|---|---|---|---|
| [MONO-01](mono-01-monorepo.md) | Monorepo restructure + isolated CI | — | — |
| [BE-01](be-01-safety-net.md) | Safety net: CI + characterization tests | — | FE-01 |
| [BE-02](be-02-foundation-config-db.md) | Foundation: profiles, secrets, Flyway | BE-01 | FE-01 |
| [BE-03](be-03-money-integrity.md) | Money integrity: transactions, atomic balance, Pix, recharge | BE-02 | none (touches the same services as BE-04) |
| [BE-04](be-04-auth-and-ownership.md) | Auth0 authentication, CurrentUser, ownership | BE-03 | FE-02 |
| [BE-05](be-05-behaviour-fixes.md) | Dates, validation and error handling | BE-04 | none |
| [BE-06](be-06-api-contract-and-evolution.md) | OpenAPI contract, generator, new endpoints | BE-05 | none |
| [BE-07](be-07-platform-upgrade.md) | Platform upgrade: Boot 3.5.x, Java 21, Java 25 | BE-06 | none |
| [BE-08](be-08-cleanup-hardening.md) | Cleanup and portfolio-grade hardening | BE-07 | FE-05 |
| [FE-01](fe-01-design-prototype.md) | UI/UX prototype and templates (designer agent) | — | BE-01, BE-02 |
| [FE-02](fe-02-foundation.md) | Angular foundation: scaffold, core, design system | BE-02, FE-01 | BE-04 |
| [FE-03](fe-03-auth-home-statement.md) | Auth, Home, Statement and detail | FE-02, BE-04, BE-06 | FE-04 (after FE-03 auth lands) |
| [FE-04](fe-04-operations.md) | Pix, Deposit, Recharge, Withdrawal | FE-03, BE-03 | none |
| [FE-05](fe-05-final-validation.md) | End-to-end, accessibility and final review | FE-04, BE-07 | BE-08 |
| [FINAL-01](final-01-test-round.md) | Final test round (after all specs) | all others | — |

## Why this order
1. BE-01 first: every later spec changes behaviour; tests are the only proof of what changed.
2. BE-02 before money fixes: constraints, outbox and `external_subject` need a versioned schema.
3. BE-03 (integrity) before BE-04 (auth/ownership): ownership queries build on the atomic primitives.
4. BE-05/06 before the platform upgrade (BE-07): functional work first, upgrades last, so regressions are attributable.
5. Frontend needs: design (FE-01) → foundation (FE-02) → real auth + `accounts/me` + row ids (BE-04/06) → operations.

## Human actions required
BE-02 rotate committed DB credentials · BE-04/FE-03 add the Auth0 Action and 2 test users · BE-06 approve OpenAPI preview · FE-01 approve design · FE-02 provide the Angular project name · BE-03 provide the MaiaBank contract only if the Pix payload must change.

## Status
| Spec | Status |
|---|---|
| MONO-01 | Done (merged in PR #1, 2026-11-02) |
| BE-01 | Done (PR #1 merged 2026-11-02; api-ci green on 062cf0b) |
| BE-02 | Done (PR #3 merged 2026-11-02; api-ci green on 567c8ab). Owner still owes the rotation of the old DB credential |
| BE-03 | Implemented + verified locally (72 tests green); PR/CI pending (owner) |
| BE-04 | Draft |
| BE-05 | Draft |
| BE-06 | Draft |
| BE-07 | Draft |
| BE-08 | Draft |
| FE-01 | Review merged (PR #4); owner visual sign-off still pending |
| FE-02 | Done (PR #5 merged 2026-11-02; app-ci green on 48230d5). Carried over: axe run → FE-05, OpenAPI type generation → FE-03 |
| FE-03 | Draft |
| FE-04 | Draft |
| FE-05 | Draft |
| FINAL-01 | Draft |

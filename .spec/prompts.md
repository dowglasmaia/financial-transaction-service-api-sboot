# Ready-to-paste prompts (one per spec or slice)

Use **one fresh agent session per spec or slice** (open the PR of the previous one and merge it BEFORE starting the next), opened at the repo root. Before pasting: the spec status is *Approved* in `specs/README.md` and the *Human action before* is done. The agent must not close a spec unless its **Minimal validation** passes. After ALL specs are Done, run **FINAL-01** (final test round).

Order: `MONO-01 → BE-01 → BE-02 → BE-03 → BE-04 → (FE-01, FE-02 done) → (SL-00 ∥ ARCH-01) → ARCH-02 → SL-01 → SL-02 → SL-03 → SL-04 → SL-05 → SL-06 → BE-07 → (FE-05 ∥ BE-08) → FINAL-01`.
**`ARCH-01`/`ARCH-02` migrate the backend to the "Hexagonal Pragmática" model (D-39) with FROZEN behaviour, before the first backend slice; `ARCH-01` may run in parallel with `SL-00` (frontend only).**
**Slices (`SL-xx`) carry backend + frontend + e2e on ONE branch and are strictly sequential** (D-31). `BE-05`, `BE-06`, `FE-03`, `FE-04` and `FE-01` no longer have prompts: they were superseded (see `specs/README.md`).

## MONO-01 — mono-01-monorepo

**Human action before:** Push to GitHub and open one trivial PR touching only the API folder and one touching only `.spec/`/app folder.

```text
Execute SPEC-MONO-01 (.spec/specs/mono-01-monorepo.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do not change any content of moved files.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/mono-01-monorepo` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `api-ci` runs for an API-only change and does NOT run for an app-only change (and vice versa)
  - `docker compose build financial-transaction-api` exits 0 (already done once locally)
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## BE-01 — be-01-safety-net

**Human action before:** Approve the spec.

```text
Execute SPEC-BE-01 (.spec/specs/be-01-safety-net.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do not modify anything under `src/main`. Characterize CURRENT behaviour, including wrong behaviour, tagging defects `// characterizes F-xx`.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-01-safety-net` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `cd api-financial-transaction-service && ./mvnw -B verify` is green locally
  - `api-ci` is green on the PR
  - `git diff -- api-financial-transaction-service/src/main` is empty
  - Each rule in `memory/produto.md` ('Rules that must be preserved') maps to at least one test
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## BE-02 — be-02-foundation-config-db

**Human action before:** Rotate the committed DB credentials (human) and approve the schema constraints.

```text
Execute SPEC-BE-02 (.spec/specs/be-02-foundation-config-db.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Keep table/column names compatible so the BE-01 suite stays green.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-02-foundation-config-db` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Start the app on an EMPTY PostgreSQL twice: schema created by Flyway, data created before restart is still there
  - `git grep -n -i "password"` shows no real credential in tracked files
  - BE-01 suite still green; `ddl-auto` is `validate`
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## BE-03 — be-03-money-integrity

**Human action before:** Approve the spec.

```text
Execute SPEC-BE-03 (.spec/specs/be-03-money-integrity.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Write the failing concurrency test FIRST and show it failing before the fix.
Do not change packages. Do not credit a local Pix recipient. Keep the Kafka topic and payload byte-identical.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-03-money-integrity` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Concurrency test (N parallel debits) passes: balance never < 0 and ledger sum = balance delta
  - A debit of exactly the full balance succeeds (204); amount > balance returns 422 with the same message
  - Injected MaiaBank failure (500/timeout) leaves balance and ledger consistent
  - Recharge Kafka payload equals the BE-01 characterization
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## BE-04 — be-04-auth-and-ownership

**Human action before:** Finish Auth0 per `auth-auth0.md`: Post-Login Action attached to the Login flow, two test users, sign-ups disabled; give the agent each user's `sub` (the SPA app and API already exist).

```text
Execute SPEC-BE-04 (.spec/specs/be-04-auth-and-ownership.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Re-verify every `[VERIFY]` item in `auth-auth0.md` against current Auth0 docs and record the result.
Use the mock JWKS issuer for tests; a real Auth0 login is a manual check.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-04-auth-and-ownership` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - No token → 401; wrong issuer/audience/expired → 401; missing `customer` role → 403; unlinked `sub` → 403
  - User A requests user B's transaction id → 404; user A cannot debit B's account
  - Statement totals contain only the caller's rows
  - One manual sign-in against the real Auth0 tenant succeeds
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## ARCH-01 - arch-01-hexagonal-pilot

**Human action before:** Approve the spec. Be ready to approve (a) the class mapping table for the whole backend and (b) the security split (`CallerIdentity` port + `shared.security` receiving the authorization bean from the `account` feature). The skill `java-spring-create-module` must be the updated version.

```text
Execute SPEC-ARCH-01 (.spec/specs/arch-01-hexagonal-pilot.md) - a BACKEND-ONLY architecture migration with FROZEN behaviour (D-39, D-41).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (decisions D-39..D-43), then the skill ~/.claude/skills/java-spring-create-module/SKILL.md with references/architecture.md, references/decision-guide.md and references/migration.md. Follow the skill in mode `migrate` (its scripts: scaffold-module.mjs, check-architecture.mjs).
Process: follow .spec/spec-execution-flow.md section 1 and the ARCH-01 runbook in section 4. Tests first (domain tests red before the domain class exists), one commit per task (`ARCH-01: <task>`), `git mv` for every move, NEVER mix a move with a behaviour change, tick a task only with an evidence line (.spec/shared/como-executar.md).
Behaviour is frozen: same HTTP statuses/JSON/messages, same OpenAPI file, same Flyway schema, same Kafka topic and payload bytes, test count not lower and no test deleted. Present the mapping table and the security split and STOP for approval before moving any code. Migrate only the `account` feature, foundation and shared kit here; the `transaction` side is ARCH-02. The atomic conditional debit stays one atomic SQL update behind a port method.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/arch-01-hexagonal-pilot` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually. Never commit or push to main.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `./mvnw -B verify` green; state the test count versus the baseline
  - `node ~/.claude/skills/java-spring-create-module/scripts/check-architecture.mjs --src api-financial-transaction-service/src/main/java --base-package com.dowglasmaia.financialtransaction --legacy controllers,services,entity,repository,config,exeptions,utils` -> 0 violations
  - `git diff main -- api-financial-transaction-service/src/main/resources` is empty; Kafka payload and the ownership/auth matrix tests are green and unchanged
  - `docker compose up` starts and `/actuator/health` is UP; one authenticated call returns the same status as before (state what was run)
  - `api-ci` green on the PR
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, sync .spec/memory (code map), commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## ARCH-02 - arch-02-hexagonal-transaction

**Human action before:** ARCH-01 is merged. Be ready to approve the transaction-side mapping (service split by D4, cross-feature relations as plain id columns, exception class names, dead classes).

```text
Execute SPEC-ARCH-02 (.spec/specs/arch-02-hexagonal-transaction.md) - a BACKEND-ONLY architecture migration with FROZEN behaviour (D-39, D-41).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (decisions D-39..D-43), then the skill ~/.claude/skills/java-spring-create-module/SKILL.md with references/architecture.md, references/decision-guide.md and references/migration.md. Follow the skill in mode `migrate` (its scripts: scaffold-module.mjs, check-architecture.mjs).
Process: follow .spec/spec-execution-flow.md section 1 and the ARCH-02 runbook in section 4. Tests first (domain tests red before the domain class exists), one commit per task (`ARCH-02: <task>`), `git mv` for every move, NEVER mix a move with a behaviour change, tick a task only with an evidence line (.spec/shared/como-executar.md).
Behaviour is frozen: same HTTP statuses/JSON/messages, same OpenAPI file, same Flyway schema, same Kafka topic and payload bytes, test count not lower and no test deleted. Money rules do not change: the atomic debit stays one atomic update, Pix keeps reserve -> commit -> MaiaBank -> confirm/compensate with the remote call OUTSIDE any database transaction, recharge keeps debit + ledger + outbox in one transaction and the relay publishes identical bytes. Typo fixes of the `exeptions` classes go in their own commit.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/arch-02-hexagonal-transaction` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually. Never commit or push to main.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `./mvnw -B verify` green; state the test count versus the baseline
  - `node ~/.claude/skills/java-spring-create-module/scripts/check-architecture.mjs --src api-financial-transaction-service/src/main/java --base-package com.dowglasmaia.financialtransaction --strict` -> 0 violations; `ArchitectureTest` green with an empty legacy list
  - Kafka payload, concurrency and failure-injection tests are green and unchanged; `git diff main -- api-financial-transaction-service/src/main/resources` is empty
  - Compose smoke reported as executed or not: health UP, one authenticated read, one write, the recharge event visible in Kafdrop
  - `api-ci` green on the PR
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, sync .spec/memory (code map), commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## SL-00 - sl-00-visual-v2

**Human action before:** (Optional) export screenshots of every v2 screen/state into `prototype/screens-v2/` if the HTML does not render locally. Be ready to give a written sign-off of the look at the end.

```text
Execute SPEC-SL-00 (.spec/specs/sl-00-visual-v2.md) - a VERTICAL SLICE: backend + frontend + e2e on ONE branch.
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including decisions D-31..D-38 and 'Frontend: prototype and agent autonomy'), the slice loop in .spec/spec-execution-flow.md section 1b, and open the CURRENT visual reference prototype/Maia Bank UI prototype/Maia Bank v2.dc.html (the v1 file is history). UI copy is English and the brand is Maia Bank (D-33).
Process: follow .spec/spec-execution-flow.md section 1b in order (contract first -> backend red/green -> API types -> frontend red/green -> e2e -> browser proof). Tests first (show them failing before the fix), one commit per task (`SL-00: <task>`), tick a task only with an evidence line (.spec/shared/como-executar.md).
Frontend only: no backend change, no feature route, no HTTP. Replace the v1 look completely (tokens, shared components, shell, /design). Do not build any v2 element that has no endpoint (D-35).
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `slice/00-visual-v2` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually. Never commit or push to main.
Stay strictly in the slice scope; stop and ask at its 'Stop and ask' items. A contract change needs the openapi-rest-contract preview and explicit approval BEFORE anything is generated or coded.
Local stack: `docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api` (Postgres on localhost:5433), API on 8088 with profile local, app on 4200. You never type a password: automated e2e uses the mock IdP; the real Auth0 login is a manual step for the owner.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `cd app-financial-transaction-service && npm ci && npm run build && npm test` green (state the counts)
  - grep for the v1 palette (CEA45C, 2A1708, FBF1DE, 5FA899) in src returns nothing
  - contrast unit test green in dark and derived light (text >= 4.5:1, controls >= 3:1)
  - screenshots of /design at 1280/768/390 in both themes; written owner sign-off of the v2 look
  - `app-ci` green on the PR
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, sync .spec/memory and app docs/prototype-deviations.md, commit on the slice branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## SL-01 - sl-01-login-and-account

**Human action before:** In Auth0 (app Transaction-app, SPA) add `http://localhost:4200` to Allowed Callback URLs, Logout URLs and Web Origins; the roles Action is attached to the Login flow; both test users exist with role customer. Run the API with `SPRING_PROFILES_ACTIVE=local`, `SEED_OWNER_SUB`, `SEED_SECOND_SUB`, and the compose infra up. Be ready to sign in manually at the end.

```text
Execute SPEC-SL-01 (.spec/specs/sl-01-login-and-account.md) - a VERTICAL SLICE: backend + frontend + e2e on ONE branch.
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including decisions D-31..D-38 and 'Frontend: prototype and agent autonomy'), the slice loop in .spec/spec-execution-flow.md section 1b, and open the CURRENT visual reference prototype/Maia Bank UI prototype/Maia Bank v2.dc.html (the v1 file is history). UI copy is English and the brand is Maia Bank (D-33).
Process: follow .spec/spec-execution-flow.md section 1b in order (contract first -> backend red/green -> API types -> frontend red/green -> e2e -> browser proof). Tests first (show them failing before the fix), one commit per task (`SL-01: <task>`), tick a task only with an evidence line (.spec/shared/como-executar.md).
Verify `@auth0/auth0-angular` against Angular 21 zoneless before using it (stop and ask if incompatible or if tokens must leave memory). New devDependencies (Playwright, type generator) and the mock-IdP image are 'Stop and ask'. Tokens never go to URLs, console or logs.
Backend structure (D-39): create or change backend code with the skill ~/.claude/skills/java-spring-create-module (mode `create`; feature placement domain/application/infra, one service per feature, ports only for outputs). Keep `ArchitectureTest` green and run its check-architecture.mjs --strict before closing. ARCH-02 must be merged.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `slice/01-login-and-account` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually. Never commit or push to main.
Stay strictly in the slice scope; stop and ask at its 'Stop and ask' items. A contract change needs the openapi-rest-contract preview and explicit approval BEFORE anything is generated or coded.
Local stack: `docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api` (Postgres on localhost:5433), API on 8088 with profile local, app on 4200. You never type a password: automated e2e uses the mock IdP; the real Auth0 login is a manual step for the owner.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `./mvnw -B verify` green; `GET /accounts/me` returns exactly `{agency, number, balance}` for the caller; no token -> 401; unlinked sub -> 403
  - `npm ci && npm run build && npm test` green; `npm run api:types` reproducible
  - e2e with the mock IdP green on 1280/768/390: unauthenticated -> landing; sign in -> Home shows the caller's balance; user 2 sees a different account; sign out; expired token -> session-expired dialog
  - axe: no serious violations on landing, sign-in, Home
  - OWNER: one real Auth0 sign-in per test user in the browser (this closes BE-04's manual validation)
  - `api-ci` and `app-ci` green on the PR
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, sync .spec/memory and app docs/prototype-deviations.md, commit on the slice branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## SL-02 - sl-02-statement-and-detail

**Human action before:** Choose the time zone for day boundaries (default America/Sao_Paulo); decide 400 vs 422 (default 422); decide the proposed `GET /transactions/summary`; be ready to approve the OpenAPI preview.

```text
Execute SPEC-SL-02 (.spec/specs/sl-02-statement-and-detail.md) - a VERTICAL SLICE: backend + frontend + e2e on ONE branch.
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including decisions D-31..D-38 and 'Frontend: prototype and agent autonomy'), the slice loop in .spec/spec-execution-flow.md section 1b, and open the CURRENT visual reference prototype/Maia Bank UI prototype/Maia Bank v2.dc.html (the v1 file is history). UI copy is English and the brand is Maia Bank (D-33).
Process: follow .spec/spec-execution-flow.md section 1b in order (contract first -> backend red/green -> API types -> frontend red/green -> e2e -> browser proof). Tests first (show them failing before the fix), one commit per task (`SL-02: <task>`), tick a task only with an evidence line (.spec/shared/como-executar.md).
Preview ALL contract changes (every operation) with the openapi-rest-contract skill and wait for approval. Migrate to openapi-generator (jakarta) keeping generated package names; record whether bean validation is now enforced. Keep every existing message and the 204/404/422 contract. Totals come from the backend; the UI never computes a balance.
Backend structure (D-39): create or change backend code with the skill ~/.claude/skills/java-spring-create-module (mode `create`; feature placement domain/application/infra, one service per feature, ports only for outputs). Keep `ArchitectureTest` green and run its check-architecture.mjs --strict before closing. ARCH-02 must be merged.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `slice/02-statement-and-detail` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually. Never commit or push to main.
Stay strictly in the slice scope; stop and ask at its 'Stop and ask' items. A contract change needs the openapi-rest-contract preview and explicit approval BEFORE anything is generated or coded.
Local stack: `docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api` (Postgres on localhost:5433), API on 8088 with profile local, app on 4200. You never type a password: automated e2e uses the mock IdP; the real Auth0 login is a manual step for the owner.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `./mvnw -B verify` green: single-day range returns that day; bad/missing/reversed dates -> 422, never 500; rows have `id`; page/size/sort/type work; totals independent of page; statement uses 2 queries
  - contract validation scripts pass, Maven generation succeeds, `openapi-diff` runs in api-ci
  - `npm ci && npm run build && npm test` green; e2e green on 3 viewports incl. user 2 opening user 1's id -> not found; axe no serious violations
  - browser proof: single-day filter, detail by id, Home summary equals Statement totals
  - `api-ci` and `app-ci` green on the PR
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, sync .spec/memory and app docs/prototype-deviations.md, commit on the slice branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## SL-03 - sl-03-withdrawal

**Human action before:** Approve the idempotency design (optional header; same key+body replays; same key+different body -> 422; per customer; TTL 24 h).

```text
Execute SPEC-SL-03 (.spec/specs/sl-03-withdrawal.md) - a VERTICAL SLICE: backend + frontend + e2e on ONE branch.
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including decisions D-31..D-38 and 'Frontend: prototype and agent autonomy'), the slice loop in .spec/spec-execution-flow.md section 1b, and open the CURRENT visual reference prototype/Maia Bank UI prototype/Maia Bank v2.dc.html (the v1 file is history). UI copy is English and the brand is Maia Bank (D-33).
Process: follow .spec/spec-execution-flow.md section 1b in order (contract first -> backend red/green -> API types -> frontend red/green -> e2e -> browser proof). Tests first (show them failing before the fix), one commit per task (`SL-03: <task>`), tick a task only with an evidence line (.spec/shared/como-executar.md).
Write the replay/concurrency tests FIRST and show them failing. The result panel (success / business failure / uncertain) and the operation shell (one in-flight request, no automatic POST retry) are shared by SL-04..06: build them reusable. Withdrawal body is `{amount}` only.
Backend structure (D-39): create or change backend code with the skill ~/.claude/skills/java-spring-create-module (mode `create`; feature placement domain/application/infra, one service per feature, ports only for outputs). Keep `ArchitectureTest` green and run its check-architecture.mjs --strict before closing. ARCH-02 must be merged.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `slice/03-withdrawal` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually. Never commit or push to main.
Stay strictly in the slice scope; stop and ask at its 'Stop and ask' items. A contract change needs the openapi-rest-contract preview and explicit approval BEFORE anything is generated or coded.
Local stack: `docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api` (Postgres on localhost:5433), API on 8088 with profile local, app on 4200. You never type a password: automated e2e uses the mock IdP; the real Auth0 login is a manual step for the owner.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Replaying a POST with the same `Idempotency-Key` does not double-debit; parallel same-key requests debit once
  - A debit of exactly the full balance succeeds (204); amount > balance returns 422 and balance is unchanged; null/zero/negative -> 422, no 500
  - Double click sends exactly one request; no automatic retry of any POST; request body is `{amount}` only
  - e2e incl. forced network failure -> uncertain panel and explicit 'Try again' reusing the key; axe no serious violations
  - `api-ci` and `app-ci` green on the PR
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, sync .spec/memory and app docs/prototype-deviations.md, commit on the slice branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## SL-04 - sl-04-deposit

**Human action before:** Approve the reconciliation: the boleto is a client-side demo and 'Simulate payment' calls the real `POST /transactions/deposit` for the caller's own account (agency and number from /accounts/me).

```text
Execute SPEC-SL-04 (.spec/specs/sl-04-deposit.md) - a VERTICAL SLICE: backend + frontend + e2e on ONE branch.
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including decisions D-31..D-38 and 'Frontend: prototype and agent autonomy'), the slice loop in .spec/spec-execution-flow.md section 1b, and open the CURRENT visual reference prototype/Maia Bank UI prototype/Maia Bank v2.dc.html (the v1 file is history). UI copy is English and the brand is Maia Bank (D-33).
Process: follow .spec/spec-execution-flow.md section 1b in order (contract first -> backend red/green -> API types -> frontend red/green -> e2e -> browser proof). Tests first (show them failing before the fix), one commit per task (`SL-04: <task>`), tick a task only with an evidence line (.spec/shared/como-executar.md).
No server-side boleto, no new endpoint. Label every boleto surface as demo. Third-party deposits stay allowed in the API but are not offered in the UI.
Backend structure (D-39): create or change backend code with the skill ~/.claude/skills/java-spring-create-module (mode `create`; feature placement domain/application/infra, one service per feature, ports only for outputs). Keep `ArchitectureTest` green and run its check-architecture.mjs --strict before closing. ARCH-02 must be merged.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `slice/04-deposit` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually. Never commit or push to main.
Stay strictly in the slice scope; stop and ask at its 'Stop and ask' items. A contract change needs the openapi-rest-contract preview and explicit approval BEFORE anything is generated or coded.
Local stack: `docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api` (Postgres on localhost:5433), API on 8088 with profile local, app on 4200. You never type a password: automated e2e uses the mock IdP; the real Auth0 login is a manual step for the owner.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Generate boleto -> Simulate payment: balance +amount exactly once and a DEPOSIT row in the statement
  - null/invalid bodies -> 422 (no NPE); unknown account -> 404 'Account not found.' shown as a field error; replay with the same key credits once
  - Double click = one request; no automatic retry; copy never claims a real boleto
  - e2e green on 3 viewports; axe no serious violations; `api-ci` and `app-ci` green on the PR
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, sync .spec/memory and app docs/prototype-deviations.md, commit on the slice branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## SL-05 - sl-05-recharge

**Human action before:** None (carriers are the backend enum VIVO/CLARO/TIM/ALGAR; v2's Oi becomes Algar).

```text
Execute SPEC-SL-05 (.spec/specs/sl-05-recharge.md) - a VERTICAL SLICE: backend + frontend + e2e on ONE branch.
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including decisions D-31..D-38 and 'Frontend: prototype and agent autonomy'), the slice loop in .spec/spec-execution-flow.md section 1b, and open the CURRENT visual reference prototype/Maia Bank UI prototype/Maia Bank v2.dc.html (the v1 file is history). UI copy is English and the brand is Maia Bank (D-33).
Process: follow .spec/spec-execution-flow.md section 1b in order (contract first -> backend red/green -> API types -> frontend red/green -> e2e -> browser proof). Tests first (show them failing before the fix), one commit per task (`SL-05: <task>`), tick a task only with an evidence line (.spec/shared/como-executar.md).
Keep the Kafka topic `recharge-mobile-topic` and payload byte-identical to the BE-01/BE-03 characterization. Success copy says the recharge was 'requested', never 'completed'.
Backend structure (D-39): create or change backend code with the skill ~/.claude/skills/java-spring-create-module (mode `create`; feature placement domain/application/infra, one service per feature, ports only for outputs). Keep `ArchitectureTest` green and run its check-architecture.mjs --strict before closing. ARCH-02 must be merged.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `slice/05-recharge` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually. Never commit or push to main.
Stay strictly in the slice scope; stop and ask at its 'Stop and ask' items. A contract change needs the openapi-rest-contract preview and explicit approval BEFORE anything is generated or coded.
Local stack: `docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api` (Postgres on localhost:5433), API on 8088 with profile local, app on 4200. You never type a password: automated e2e uses the mock IdP; the real Auth0 login is a manual step for the owner.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Recharge payload on Kafka equals the BE-01 characterization (test) and the event is visible in Kafdrop (record what you saw)
  - invalid phone/carrier/amount/null bodies -> 422 (no NPE); debit + ledger + outbox consistent
  - Success text contains 'requested' and never 'completed'; masked phone splits into areaCode + phoneNumber
  - e2e green on 3 viewports; axe no serious violations; `api-ci` and `app-ci` green on the PR
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, sync .spec/memory and app docs/prototype-deviations.md, commit on the slice branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## SL-06 - sl-06-pix

**Human action before:** Confirm `maiabank-api` is up on localhost:8089; provide the MaiaBank contract only if the Pix payload must change.

```text
Execute SPEC-SL-06 (.spec/specs/sl-06-pix.md) - a VERTICAL SLICE: backend + frontend + e2e on ONE branch.
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including decisions D-31..D-38 and 'Frontend: prototype and agent autonomy'), the slice loop in .spec/spec-execution-flow.md section 1b, and open the CURRENT visual reference prototype/Maia Bank UI prototype/Maia Bank v2.dc.html (the v1 file is history). UI copy is English and the brand is Maia Bank (D-33).
Process: follow .spec/spec-execution-flow.md section 1b in order (contract first -> backend red/green -> API types -> frontend red/green -> e2e -> browser proof). Tests first (show them failing before the fix), one commit per task (`SL-06: <task>`), tick a task only with an evidence line (.spec/shared/como-executar.md).
Do not credit a local Pix recipient. Keep reserve -> MaiaBank -> confirm/compensate and the Feign timeouts/circuit breaker as built in BE-03. Client validators must agree with the server on the shared CPF/e-mail/phone cases; never log or show a full Pix key.
Backend structure (D-39): create or change backend code with the skill ~/.claude/skills/java-spring-create-module (mode `create`; feature placement domain/application/infra, one service per feature, ports only for outputs). Keep `ArchitectureTest` green and run its check-architecture.mjs --strict before closing. ARCH-02 must be merged.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `slice/06-pix` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually. Never commit or push to main.
Stay strictly in the slice scope; stop and ask at its 'Stop and ask' items. A contract change needs the openapi-rest-contract preview and explicit approval BEFORE anything is generated or coded.
Local stack: `docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api` (Postgres on localhost:5433), API on 8088 with profile local, app on 4200. You never type a password: automated e2e uses the mock IdP; the real Auth0 login is a manual step for the owner.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - CPF/e-mail/phone/null-body cases -> 422 with existing messages and no NPE; client and server agree on the shared cases
  - A MaiaBank failure/timeout leaves balance and ledger consistent (existing BE-03 tests green); UI shows 'We couldn't send this Pix. Your balance was not changed.' or the uncertain-outcome panel
  - Valid Pix debits once and appears in the statement; replay with the same key sends one Pix
  - e2e against the real maiabank-api container green on 3 viewports; axe no serious violations; `api-ci` and `app-ci` green on the PR
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, sync .spec/memory and app docs/prototype-deviations.md, commit on the slice branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## BE-07 — be-07-platform-upgrade

**Human action before:** Approve the Boot version and JDK vendor/image. (The local build is already on Java 21 with Lombok 1.18.42, uncommitted: commit it as its own change first.)

```text
Execute SPEC-BE-07 (.spec/specs/be-07-platform-upgrade.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do it in three separate steps (Java 17 + Boot 3.5.x; then Java 21; then Java 25), each with its own green suite. Save a baseline of `dependency:tree` and Actuator/Prometheus output first.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-07-platform-upgrade` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Suite green after each step
  - `java -version` inside the runtime image prints 25
  - `docker compose up --build` starts the API and `/actuator/health` is UP
  - Metric names unchanged vs baseline
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## BE-08 — be-08-cleanup-hardening

**Human action before:** Decide the rounding rule for amounts with more than 2 decimals.

```text
Execute SPEC-BE-08 (.spec/specs/be-08-cleanup-hardening.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do not move features between packages or change the architecture (it is frozen by ARCH-01/02 and `ArchitectureTest`); work inside the existing feature modules.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-08-cleanup-hardening` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `check-architecture.mjs --strict` reports 0 violations and `ArchitectureTest` is green (no new violations)
  - Log output of a full flow contains no account number, Pix key or CPF
  - CI scans pass
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## FE-02 — fe-02-foundation

**Human action before:** FE-01 signed off (tokens tasks). Project name is fixed.

```text
Execute SPEC-FE-02 (.spec/specs/fe-02-foundation.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including 'Frontend: prototype and agent autonomy') and open the reference prototype in prototype/Maia Bank UI prototype/ (Maia Bank.dc.html).
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Project `app-financial-transaction-service` at the repo root; use the `angular-frontend-scaffold` skill (run its scaffold and validate scripts). Extract tokens from the prototype; you may improve them (log in `docs/prototype-deviations.md`).
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/fe-02-foundation` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `cd app-financial-transaction-service && npm ci && npm run build` succeeds
  - `app-ci` runs and is green on the PR
  - `/design` route shows tokens and components in dark and light
  - No `any`, no HTTP outside `data-access`
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## FE-05 — fe-05-final-validation

**Human action before:** SL-06 and BE-07 Done; release sign-off.

```text
Execute SPEC-FE-05 (.spec/specs/fe-05-final-validation.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including 'Frontend: prototype and agent autonomy') and open the reference prototype prototype/Maia Bank UI prototype/Maia Bank v2.dc.html.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Update `.spec/memory/` so it matches what was delivered.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/fe-05-final-validation` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Playwright e2e for flows F-0…F-9 green on 3 viewports
  - axe: zero serious violations
  - Demo README with run instructions exists
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## FINAL-01 — final-01-test-round

**Human action before:** All other specs Done.

```text
Execute SPEC-FINAL-01 (.spec/specs/final-01-test-round.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do not add features. Fix only defects found, each with a regression test, and re-run the affected checks.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/final-01-test-round` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Then push the branch (`git push -u origin <branch>`). Do NOT open the PR and do NOT merge: the owner opens PRs and merges manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Every item of the test round matrix passes or has an accepted, documented limitation
  - `docs/test-report.md` produced with evidence
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch and push it (no PR, no merge), and report which validations were actually executed vs only written.
```

## Rules for every prompt
- **Git:** always `git checkout main && git pull origin main` before creating the branch (`spec/<id>-<slug>` or `slice/<nn>-<slug>`); commit after the minimal validation passes, then push the branch; never open PRs or merge (the owner does it manually); never commit or push to `main`. If the dependency specs are not merged into main yet, stop and ask.
- Fresh session per spec or slice; never combine two in one session. **Backend code is created/changed with the skill `java-spring-create-module` (D-39); a package move never shares a commit with a behaviour change.** **A slice never leaves the other side for later:** if a field, status or message is needed by the frontend, the backend change is in the same slice (and the contract was previewed first).
- If a validation cannot be run (e.g. no Docker or no Auth0 tenant), the agent reports it as **not executed** and the spec stays open. The agent never types a password.
- If the agent proposes a change outside the spec, it writes a proposal and stops (backend contract wins over the prototype).
- Language: docs and code in English, chat answers in Portuguese; UI copy English; brand Maia Bank (D-33).

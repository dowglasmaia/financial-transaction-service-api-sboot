# Ready-to-paste prompts (one per spec)

Use **one fresh agent session per spec** (open the PR of the previous spec and merge it BEFORE starting the next), opened at the repo root. Before pasting: the spec status is *Approved* in `specs/README.md` and the *Human action before* is done. The agent must not close a spec unless its **Minimal validation** passes. After ALL specs are Done, run **FINAL-01** (final test round).

Order: `MONO-01 → (BE-01 ∥ FE-01) → BE-02 → (BE-03 ∥ FE-02) → BE-04 → BE-05 → BE-06 → (FE-03 ∥ BE-07) → FE-04 → (FE-05 ∥ BE-08) → FINAL-01`. Backend specs never run in parallel with each other.

## MONO-01 — mono-01-monorepo

**Human action before:** Push to GitHub and open one trivial PR touching only the API folder and one touching only `.spec/`/app folder.

```text
Execute SPEC-MONO-01 (.spec/specs/mono-01-monorepo.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do not change any content of moved files.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/mono-01-monorepo` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `api-ci` runs for an API-only change and does NOT run for an app-only change (and vice versa)
  - `docker compose build financial-transaction-api` exits 0 (already done once locally)
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## BE-01 — be-01-safety-net

**Human action before:** Approve the spec.

```text
Execute SPEC-BE-01 (.spec/specs/be-01-safety-net.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do not modify anything under `src/main`. Characterize CURRENT behaviour, including wrong behaviour, tagging defects `// characterizes F-xx`.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-01-safety-net` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `cd api-financial-transaction-service && ./mvnw -B verify` is green locally
  - `api-ci` is green on the PR
  - `git diff -- api-financial-transaction-service/src/main` is empty
  - Each rule in `memory/produto.md` ('Rules that must be preserved') maps to at least one test
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## BE-02 — be-02-foundation-config-db

**Human action before:** Rotate the committed DB credentials (human) and approve the schema constraints.

```text
Execute SPEC-BE-02 (.spec/specs/be-02-foundation-config-db.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Keep table/column names compatible so the BE-01 suite stays green.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-02-foundation-config-db` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Start the app on an EMPTY PostgreSQL twice: schema created by Flyway, data created before restart is still there
  - `git grep -n -i "password"` shows no real credential in tracked files
  - BE-01 suite still green; `ddl-auto` is `validate`
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## BE-03 — be-03-money-integrity

**Human action before:** Approve the spec.

```text
Execute SPEC-BE-03 (.spec/specs/be-03-money-integrity.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Write the failing concurrency test FIRST and show it failing before the fix.
Do not change packages. Do not credit a local Pix recipient. Keep the Kafka topic and payload byte-identical.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-03-money-integrity` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Concurrency test (N parallel debits) passes: balance never < 0 and ledger sum = balance delta
  - A debit of exactly the full balance succeeds (204); amount > balance returns 422 with the same message
  - Injected MaiaBank failure (500/timeout) leaves balance and ledger consistent
  - Recharge Kafka payload equals the BE-01 characterization
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## BE-04 — be-04-auth-and-ownership

**Human action before:** Create the Okta org, authorization server, scopes, `groups` claim, SPA app, trusted origins and test users following `auth-okta.md`.

```text
Execute SPEC-BE-04 (.spec/specs/be-04-auth-and-ownership.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Re-verify every `[VERIFY]` item in `auth-okta.md` against current Okta docs and record the result.
Use the mock JWKS issuer for tests; a real Okta login is a manual check.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-04-auth-and-ownership` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - No token → 401; wrong issuer/audience/expired → 401; missing scope → 403; unlinked `sub` → 403
  - User A requests user B's transaction id → 404; user A cannot debit B's account
  - Statement totals contain only the caller's rows
  - One manual sign-in against the real Okta dev org succeeds
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## BE-05 — be-05-behaviour-fixes

**Human action before:** Choose the time zone for day boundaries (default proposal: America/Sao_Paulo).

```text
Execute SPEC-BE-05 (.spec/specs/be-05-behaviour-fixes.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Keep all existing messages and the 204/404/422 contract.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-05-behaviour-fixes` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - A statement for a single day returns that day's transactions
  - Missing/invalid dates and null request bodies never produce 500/NPE
  - Suite green
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## BE-06 — be-06-api-contract-and-evolution

**Human action before:** Approve the OpenAPI preview (the agent must present it before generating anything).

```text
Execute SPEC-BE-06 (.spec/specs/be-06-api-contract-and-evolution.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Use the `openapi-rest-contract` skill's preview → approval → generation flow. Report whether generated bean validation is now enforced.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-06-api-contract-and-evolution` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Contract validation scripts of the skill pass and Maven generation succeeds
  - `GET /accounts/me` returns `{agency, number, balance}`; statement rows contain `id`; pagination parameters work
  - Replaying a POST with the same `Idempotency-Key` does not double-debit
  - Suite green
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## BE-07 — be-07-platform-upgrade

**Human action before:** Approve the Boot version and JDK vendor/image.

```text
Execute SPEC-BE-07 (.spec/specs/be-07-platform-upgrade.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do it in three separate steps (Java 17 + Boot 3.5.x; then Java 21; then Java 25), each with its own green suite. Save a baseline of `dependency:tree` and Actuator/Prometheus output first.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-07-platform-upgrade` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Suite green after each step
  - `java -version` inside the runtime image prints 25
  - `docker compose up --build` starts the API and `/actuator/health` is UP
  - Metric names unchanged vs baseline
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## BE-08 — be-08-cleanup-hardening

**Human action before:** Decide the rounding rule for amounts with more than 2 decimals.

```text
Execute SPEC-BE-08 (.spec/specs/be-08-cleanup-hardening.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do not move or rename any package or class.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/be-08-cleanup-hardening` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `git diff --name-status` shows no renames/moves under `api-financial-transaction-service/src`
  - Log output of a full flow contains no account number, Pix key or CPF
  - ArchUnit tests and CI scans pass
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## FE-01 — fe-01-design-prototype

**Human action before:** Open `prototype/Maia Bank UI prototype/Maia Bank.dc.html` yourself.

```text
Execute SPEC-FE-01 (.spec/specs/fe-01-design-prototype.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including 'Frontend: prototype and agent autonomy') and open the reference prototype in prototype/Maia Bank UI prototype/ (Maia Bank.dc.html).
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do not write code. Review every screen/state against `.spec/designer-brief.md` §13 and write the divergence list (prototype vs backend contract, e.g. boleto deposit; prototype vs brief).
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/fe-01-design-prototype` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Checklist §13 completed with pass/fail per item
  - Divergence list written (seed for `docs/prototype-deviations.md`)
  - Written sign-off from the owner
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## FE-02 — fe-02-foundation

**Human action before:** FE-01 signed off (tokens tasks). Project name is fixed.

```text
Execute SPEC-FE-02 (.spec/specs/fe-02-foundation.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including 'Frontend: prototype and agent autonomy') and open the reference prototype in prototype/Maia Bank UI prototype/ (Maia Bank.dc.html).
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Project `app-financial-transaction-service` at the repo root; use the `angular-frontend-scaffold` skill (run its scaffold and validate scripts). Extract tokens from the prototype; you may improve them (log in `docs/prototype-deviations.md`).
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/fe-02-foundation` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - `cd app-financial-transaction-service && npm ci && npm run build` succeeds
  - `app-ci` runs and is green on the PR
  - `/design` route shows tokens and components in dark and light
  - No `any`, no HTTP outside `data-access`
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## FE-03 — fe-03-auth-home-statement

**Human action before:** BE-04 and BE-06 are Done (real auth, `/accounts/me`, row ids).

```text
Execute SPEC-FE-03 (.spec/specs/fe-03-auth-home-statement.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including 'Frontend: prototype and agent autonomy') and open the reference prototype in prototype/Maia Bank UI prototype/ (Maia Bank.dc.html).
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Use `angular-module-scaffold` per feature with a `doc.md`. The prototype is a reference, not a limit: apply Tier-1 improvements and log them; write Tier-2 proposals and stop.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/fe-03-auth-home-statement` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - e2e with the mock issuer: unauthenticated route redirects to login; after login Home shows the balance from `/accounts/me`
  - Statement: filter a single day, see rows, open detail by id
  - No tokens in URLs/console; axe has no serious violations on these screens
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## FE-04 — fe-04-operations

**Human action before:** Approve the deposit reconciliation if the prototype's boleto flow must change (Tier-2 proposal from FE-01/FE-03).

```text
Execute SPEC-FE-04 (.spec/specs/fe-04-operations.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including 'Frontend: prototype and agent autonomy') and open the reference prototype in prototype/Maia Bank UI prototype/ (Maia Bank.dc.html).
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Order: Withdrawal → Deposit → Recharge → Pix. Build the shared result panel first (success / business failure / uncertain outcome).
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/fe-04-operations` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Each flow completes against `docker compose` (balance changes, new row in statement)
  - Double click sends exactly one request; no automatic retry of any POST
  - Withdrawal request body is `{amount}` only
  - Recharge success text says 'requested', never 'completed'
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## FE-05 — fe-05-final-validation

**Human action before:** Release sign-off.

```text
Execute SPEC-FE-05 (.spec/specs/fe-05-final-validation.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md (including 'Frontend: prototype and agent autonomy') and open the reference prototype in prototype/Maia Bank UI prototype/ (Maia Bank.dc.html).
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Update `.spec/memory/` so it matches what was delivered.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/fe-05-final-validation` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Playwright e2e for flows F-0…F-9 green on 3 viewports
  - axe: zero serious violations
  - Demo README with run instructions exists
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## FINAL-01 — final-01-test-round

**Human action before:** All other specs Done.

```text
Execute SPEC-FINAL-01 (.spec/specs/final-01-test-round.md).
Read first: .spec/memory/produto.md and .spec/memory/contexto-tecnico.md.
Process: follow .spec/spec-execution-flow.md section 1 (the loop) and the runbook for this spec in section 4. Tests first, one commit per task, tick a task only with an evidence line (.spec/shared/como-executar.md).
Do not add features. Fix only defects found, each with a regression test, and re-run the affected checks.
Git: FIRST run `git checkout main && git pull origin main`, then create the branch `spec/final-01-test-round` from main (if main lacks a dependency spec, stop and ask). Commit when the spec's minimal validation passes (one commit per task while working). Do NOT push and do NOT open a PR: the owner opens PRs manually.
Stay strictly in the spec scope; stop and ask at its 'Stop and ask' items.
Minimal validation (you may NOT mark the spec Done until all pass; paste the real outputs in the PR):
  - Every item of the test round matrix passes or has an accepted, documented limitation
  - `docs/test-report.md` produced with evidence
Finish: fill the spec's Result section, set its status in .spec/specs/README.md, commit on the spec branch (no push, no PR), and report which validations were actually executed vs only written.
```

## Rules for every prompt
- **Git:** always `git checkout main && git pull origin main` before creating the spec branch; commit after the spec's minimal validation passes; never push or open PRs (the owner does it manually). If the dependency specs are not merged into main yet, stop and ask.
- Fresh session per spec; never combine two specs in one session.
- If a validation cannot be run (e.g. no Docker or no Okta org), the agent reports it as **not executed** and the spec stays open.
- If the agent proposes a change outside the spec, it writes a proposal and stops (backend contract wins over the prototype).

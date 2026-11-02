# SPEC-BE-02 — Foundation: profiles, secrets, Flyway

**Status:** Implemented and verified locally — commits ready on the branch; PR/CI pending (owner) · **Depends on:** BE-01 · **Parallel with:** FE-01
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Stop the schema wipe and committed credentials; give the DB a versioned schema.

## Scope
- Profiles `local` / `test` / `prod`; remove forced `test` default
- Env-based datasource/Kafka config; no secrets in tracked files; `application-local.yaml.example`; fix `.gitignore` contradictions
- Flyway `V1__baseline.sql` (clean schema): unique `(agency_id, number)`, FKs on `transaction`, `CHECK (balance >= 0)`, NOT NULL money `numeric(19,2)`, index `(account_id, date_hour desc)`
- `ddl-auto: validate`; seed only in `local`; `show-sql` off; pin Docker image tags

**Out of scope:** Auth, business logic

## Tasks
- [x] Write `V1` from entities (compare with Hibernate DDL)
  > ✅ 2026-11-02 17:15 +0000 — `db/migration/V1__baseline.sql` (agency, usuario, account, transaction; unique `(agency_id, number)`, FKs, `CHECK balance >= 0`, NOT NULL money `numeric(19,2)`, type CHECK, index `(account_id, date_hour desc)`). Verification: Hibernate `validate` passes against it (app boots; 52 tests). Desvio: **no FK on `transaction.account_id`** — recharge still stores the account number there (F-08, pinned by BE-01); BE-03 adds the FK after fixing it.
- [x] Add Flyway + validate mode
  > ✅ 2026-11-02 17:15 +0000 — `flyway-core` (Boot-managed) added; `ddl-auto: validate`. Verification: new `FlywaySchemaTest` was run RED first (7 failures: no Flyway, ddl-auto create, no constraints), then GREEN (7/7); full suite 52 tests, 0 failures, BUILD SUCCESS.
- [x] Split config per profile with env placeholders
  > ✅ 2026-11-02 17:15 +0000 — `application.yaml` reads `DB_URL`/`DB_USER`/`DB_PASSWORD` (required), `KAFKA_BOOTSTRAP_SERVERS`, `MAIABANK_URL`; no forced profile; `show-sql` false; added `application-local.yaml.example`. Verification: app started in containers with env only (see minimal validation).
- [x] Limit `DBService`/`TestConfigDb` to `local` (temporary adapter until BE-04)
  > ✅ 2026-11-02 17:15 +0000 — `TestConfigDb` is `@Profile({"local","test"})` (test needed for the automated suite) and `DBService` seed is idempotent (find-or-create), so restarts on an existing DB work; seed user replaced by obviously fake data (`Demo User`, `demo.user@example.test`). Verification: restart test below. Note: outside local/test the acting account cannot be resolved until BE-04.
- [ ] Remove credentials; rotate them (human)
  > ⏳ 2026-11-02 17:15 +0000 — Parcial: credentials removed from tracked files (`application.yaml`, `docker-compose.yml`; `.gitignore` no longer hides tracked files and ignores `application-local.yaml`/`.env`). Falta: **rotation is a human action** — the old dev credential `maia/maiapw` remains in git history; treat it as burned and change it anywhere it was reused. Próxima ação: owner confirms rotation.
- [x] Update docker-compose
  > ✅ 2026-11-02 17:15 +0000 — credentials via `.env` (`${VAR:?}` fails fast when missing), profile `local`, env-based DB/Kafka/MaiaBank, Kafka/Zookeeper/Kafdrop tags pinned (7.6.1/7.6.1/4.0.2), obsolete `version` and unused volumes removed, `.env.example` added. Verification: `docker compose --env-file .env.example config -q` OK; without `.env` it errors as intended. Not run: full `docker compose up`. `dowglasmaia/maiabank` stays unpinned (external image, owner's).
- [x] Adapt tests to Flyway
  > ✅ 2026-11-02 17:15 +0000 — no test change was needed beyond the new `FlywaySchemaTest`: the BE-01 suite runs unchanged on the Flyway schema (Hibernate validates it). Verification: 52/52.

## Acceptance
- [x] App starts on empty PostgreSQL via Flyway; restart keeps data
  > ✅ 2026-11-02 17:15 +0000 — see minimal validation (executed with containers).
- [x] No real credential in tracked files
  > ✅ 2026-11-02 17:15 +0000 — `git grep -n -i password` (working tree) lists only: `${DB_PASSWORD}`/`${POSTGRES_PASSWORD}` placeholders, `.env.example` and `application-local.yaml.example` (`change-me`), the Maven wrapper's `MVNW_PASSWORD` script variables, and the test's container password. History still contains `maiapw` (rotation, above).
- [x] BE-01 suite green
  > ✅ 2026-11-02 17:15 +0000 — 52 tests (45 BE-01 + 7 new), 0 failures, BUILD SUCCESS (`mvn -B -ntp verify` in `maven:3.9-eclipse-temurin-17` with Testcontainers).

## Minimal validation (required to close this spec)
- [x] Start the app on an EMPTY PostgreSQL twice: schema created by Flyway, data created before restart is still there
  > ✅ 2026-11-02 17:15 +0000 — EXECUTED: empty `postgres:14.12` container + jar (`amazoncorretto:17`, profile `local`, env-only config). Run 1: Flyway 'Migrating schema public to version 1 - baseline… Successfully applied 1 migration'; `POST /deposit 200.00` → 204. App stopped, run 2 on the SAME database: 'Schema "public" is up to date. No migration necessary.'; DB state: balance=5200.00, ledger_rows=1, users=1, accounts=1, agencies=1 (seed not duplicated), `GET /transactions` returned the DEPOSIT row and balance 5200.00. Containers/network removed afterwards.
- [x] `git grep -n -i "password"` shows no real credential in tracked files
  > ✅ 2026-11-02 17:15 +0000 — see acceptance item above.
- [x] BE-01 suite still green; `ddl-auto` is `validate`
  > ✅ 2026-11-02 17:15 +0000 — 52/52 green; `FlywaySchemaTest.hibernateOnlyValidatesTheSchema` asserts `validate`.

## Stop and ask
- Credential rotation (human)
- Accept schema constraints

## Skills
java-persistence-performance (Flyway, indexes)

## Result
Schema is now versioned (Flyway V1) and only validated by Hibernate; no secrets in tracked files; seed is demo-only and idempotent; compose is env-driven with pinned tags. Executed: full suite (52/52) and a two-start restart test on an empty PostgreSQL. **Not done / needs the owner:** credential rotation (human), PR + `api-ci` run, full `docker compose up`. Accepted limitations: no FK on `transaction.account_id` until BE-03 (F-08); outside `local`/`test` the acting account is unresolved until BE-04; `dowglasmaia/maiabank` image tag unpinned.

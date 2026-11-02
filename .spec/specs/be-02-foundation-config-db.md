# SPEC-BE-02 — Foundation: profiles, secrets, Flyway

**Status:** Draft (not authorized) · **Depends on:** BE-01 · **Parallel with:** FE-01
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
- [ ] Write `V1` from entities (compare with Hibernate DDL)
- [ ] Add Flyway + validate mode
- [ ] Split config per profile with env placeholders
- [ ] Limit `DBService`/`TestConfigDb` to `local` (temporary adapter until BE-04)
- [ ] Remove credentials; rotate them (human)
- [ ] Update docker-compose
- [ ] Adapt tests to Flyway

## Acceptance
- [ ] App starts on empty PostgreSQL via Flyway; restart keeps data
- [ ] No real credential in tracked files
- [ ] BE-01 suite green

## Minimal validation (required to close this spec)
- [ ] Start the app on an EMPTY PostgreSQL twice: schema created by Flyway, data created before restart is still there
- [ ] `git grep -n -i "password"` shows no real credential in tracked files
- [ ] BE-01 suite still green; `ddl-auto` is `validate`

## Stop and ask
- Credential rotation (human)
- Accept schema constraints

## Skills
java-persistence-performance (Flyway, indexes)

## Result
_Filled on completion: result, checks run, accepted limitations._

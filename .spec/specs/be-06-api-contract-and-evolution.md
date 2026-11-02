# SPEC-BE-06 — OpenAPI contract, generator, new endpoints

**Status:** Draft (not authorized) · **Depends on:** BE-05 · **Parallel with:** none
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Correct contract drift, move to openapi-generator (jakarta) so validation is enforced, and add the approved additive features.

## Scope
- Switch swagger-codegen → openapi-generator (jakarta); prove whether generated validation is enforced (answers the `javax` question)
- Contract: document 204/404/422/401/403, enums, `bearerAuth`, server URL; remove unused schemas; decide 400 vs 422 for field validation
- Add `GET /accounts/me`, row `id`, `page/size/sort/type`, aggregate totals (2 queries), ISO date params (old ones deprecated), optional `Idempotency-Key` (Flyway `V4`)

**Out of scope:** Breaking changes to existing fields/params

## Tasks
- [ ] PREVIEW all contract changes and obtain explicit approval (skill rule)
- [ ] Generator migration
- [ ] Apply corrections
- [ ] Implement new endpoints/queries as projections + statement-count assertions
- [ ] Idempotency store + replay test
- [ ] openapi-diff check in CI

## Acceptance
- [ ] Contract matches real behaviour for every operation/status
- [ ] Existing valid requests behave identically
- [ ] Frontend can render Home and Statement/detail from the contract only
- [ ] Replay with same key does not double-debit

## Minimal validation (required to close this spec)
- [ ] Contract validation scripts of the skill pass and Maven generation succeeds
- [ ] `GET /accounts/me` returns `{agency, number, balance}`; statement rows contain `id`; pagination parameters work
- [ ] Replaying a POST with the same `Idempotency-Key` does not double-debit
- [ ] Suite green

## Stop and ask
- Contract preview approval
- 400 vs 422 decision
- New table/index

## Skills
openapi-rest-contract, java-persistence-performance

## Result
_Filled on completion: result, checks run, accepted limitations._

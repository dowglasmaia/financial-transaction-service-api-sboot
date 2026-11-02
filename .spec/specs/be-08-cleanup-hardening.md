# SPEC-BE-08 — Cleanup and portfolio-grade hardening

**Status:** Draft (not authorized) · **Depends on:** BE-07 · **Parallel with:** FE-05
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Tidy internals and add the hygiene a portfolio reviewer expects, without changing packages.

## Scope
- `Money` value type (decide rounding for >2 decimals); `Account` invariants in the entity; remove dead code (`TransactionRequestDTO`, `AccountModel`, MapStruct leftovers); ArchUnit freezing current layering
- Logs without PII (mask account/key/CPF, no entity `toString`), correlation id; actuator on separate port; business metrics; Docker healthchecks; dependency + secret scan in CI

**Out of scope:** Package/class moves (forbidden); Production compliance

## Tasks
- [ ] Money + Account invariants
- [ ] Delete dead code
- [ ] ArchUnit tests
- [ ] Log audit + masking
- [ ] Management port + metrics
- [ ] CI scans

## Acceptance
- [ ] `git diff --name-status` shows no moves/renames
- [ ] No PII in logs (grep + test)
- [ ] Scans run in CI with no unwaived HIGH/CRITICAL

## Minimal validation (required to close this spec)
- [ ] `git diff --name-status` shows no renames/moves under `api-financial-transaction-service/src`
- [ ] Log output of a full flow contains no account number, Pix key or CPF
- [ ] ArchUnit tests and CI scans pass

## Stop and ask
- Rounding rule (may reject inputs)
- Scan tool choice

## Skills
java-modern-engineering

## Result
_Filled on completion: result, checks run, accepted limitations._

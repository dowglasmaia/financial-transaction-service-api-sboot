# SPEC-BE-08 — Cleanup and portfolio-grade hardening

**Status:** Draft (not authorized) · **Depends on:** BE-07 (and ARCH-02) · **Parallel with:** FE-05
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Tidy internals and add the hygiene a portfolio reviewer expects, inside the feature-module structure created by ARCH-01/02 (D-39).

## Scope
- `Money` value type (decide rounding for >2 decimals); `Account` invariants in `account.domain`; remove dead code (`TransactionRequestDTO`, `AccountModel`, MapStruct leftovers); extend the ArchUnit rules installed by ARCH-01/02 if a new rule is worth freezing
- Logs without PII (mask account/key/CPF, no entity `toString`), correlation id; actuator on separate port; business metrics; Docker healthchecks; dependency + secret scan in CI

**Out of scope:** Moving features between packages or changing the architecture (done in ARCH-01/02; the model is frozen by `ArchitectureTest`); Production compliance

## Tasks
- [ ] Money + Account invariants
- [ ] Delete dead code
- [ ] ArchUnit rules reviewed (extend only if needed; keep the legacy list empty)
- [ ] Log audit + masking
- [ ] Management port + metrics
- [ ] CI scans

## Acceptance
- [ ] `ArchitectureTest` and `check-architecture.mjs --strict` stay green; no class moved between features
- [ ] No PII in logs (grep + test)
- [ ] Scans run in CI with no unwaived HIGH/CRITICAL

## Minimal validation (required to close this spec)
- [ ] `check-architecture.mjs --strict` → 0 violations and `ArchitectureTest` green (no new violations introduced)
- [ ] Log output of a full flow contains no account number, Pix key or CPF
- [ ] CI scans pass

## Stop and ask
- Rounding rule (may reject inputs)
- Scan tool choice

## Skills
java-modern-engineering

## Result
_Filled on completion: result, checks run, accepted limitations._

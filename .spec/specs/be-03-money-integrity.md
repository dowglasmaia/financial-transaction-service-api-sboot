# SPEC-BE-03 — Money integrity: transactions, atomic balance, Pix, recharge

**Status:** Draft (not authorized) · **Depends on:** BE-02 · **Parallel with:** none (touches the same services as BE-04)
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Make every money operation atomic and race-safe and apply the decided `>=` rule, keeping packages unchanged.

## Scope
- `@Transactional` on deposit, withdraw, recharge, Pix
- Atomic conditional debit (`UPDATE … WHERE balance >= :amt`) or `@Version` + bounded retry; single balance rule; fix misleading `addAmountToAccount`
- `TransactionService.save` takes `TransactionTypeEnum` (no `Object`/`instanceof`)
- Deposit: remove catch-all that swallows ledger failure
- Recharge: ledger `accountId` = account id; Kafka via transactional outbox (Flyway `V2`), payload/topic unchanged
- Pix: reserve (debit + ledger PENDING) → commit → MaiaBank → confirm/compensate; Feign timeouts; verify circuit breaker works; fill ledger `agencyId`; ledger `status` column (additive)

**Out of scope:** Ownership/auth (BE-04); MaiaBank payload change (needs its contract); Crediting a local Pix recipient (decided: no)

## Tasks
- [ ] Failing concurrency test (N parallel debits: `balance >= 0` and ledger Σ = Δ balance)
- [ ] Atomic debit/credit + single rule
- [ ] Transaction boundaries (no self-invocation)
- [ ] Refactor `save` signature
- [ ] Deposit failure rolls back
- [ ] Outbox + relay + Flyway `V2`
- [ ] Pix reserve/confirm/compensate + failure-injection tests (MaiaBank 500, timeout, open breaker, DB failure)
- [ ] Flip tests: exact-balance debit allowed

## Acceptance
- [ ] Concurrency test fails before, passes after
- [ ] Exact-balance debit succeeds; amount > balance → 422 same message
- [ ] After any injected failure balance and ledger are consistent
- [ ] Recharge payload byte-identical to BE-01
- [ ] No package moved

## Minimal validation (required to close this spec)
- [ ] Concurrency test (N parallel debits) passes: balance never < 0 and ledger sum = balance delta
- [ ] A debit of exactly the full balance succeeds (204); amount > balance returns 422 with the same message
- [ ] Injected MaiaBank failure (500/timeout) leaves balance and ledger consistent
- [ ] Recharge Kafka payload equals the BE-01 characterization

## Stop and ask
- Any behaviour change beyond `>=` and the items above
- MaiaBank request body change

## Skills
java-persistence-performance (transactions-concurrency, Flyway), java-modern-engineering

## Result
_Filled on completion: result, checks run, accepted limitations._

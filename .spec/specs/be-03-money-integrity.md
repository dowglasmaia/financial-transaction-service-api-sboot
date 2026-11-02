# SPEC-BE-03 — Money integrity: transactions, atomic balance, Pix, recharge

**Status:** Implemented and verified locally; PR + `api-ci` pending (owner) · **Depends on:** BE-02 · **Parallel with:** none (touches the same services as BE-04)
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
- [x] Failing concurrency test (N parallel debits: `balance >= 0` and ledger Σ = Δ balance)
  > ✅ 2026-11-02 18:57 +0000 — `integrity/MoneyConcurrencyTest` (24 parallel withdrawals of 300.00; 12 deposits + 12 withdrawals of 700.00). Run RED first against the unchanged code (commit `e63f6c5`): both tests failed with lost updates — e.g. 24 ledger rows (7200.00) while the balance only dropped by 1200.00; mixed run: expected balance -2200.00 vs actual 2900.00.
- [x] Atomic debit/credit + single rule
  > ✅ 2026-11-02 18:57 +0000 — `AccountRepository.debit` = `UPDATE … SET balance = balance - :amount WHERE id = :id AND balance >= :amount` (0 rows = insufficient) and atomic `credit`; the balance rule now exists in ONE place, used by withdraw, Pix and recharge through `AddAmountService.debit/debitById` (method renamed from the misleading `addAmountToAccount`; the class name is kept per D-19). Verification: concurrency tests now green (exactly 16 of 24 accepted, balance 200.00, Σ ledger = Δ balance).
- [x] Transaction boundaries (no self-invocation)
  > ✅ 2026-11-02 18:57 +0000 — `@Transactional` on deposit, withdraw and recharge (public interface methods, proxied calls); Pix uses `TransactionTemplate` for its two short transactions because the remote call must stay OUTSIDE any DB transaction (no self-invocation involved). Verification: `FailureInjectionTest` (6 tests) — a ledger/outbox failure rolls back deposit, withdrawal, recharge and the Pix reservation; balance unchanged, no event, MaiaBank not called.
- [x] Refactor `save` signature
  > ✅ 2026-11-02 18:57 +0000 — `TransactionService.save(TransactionTypeEnum, …)` (no `Object`/`instanceof`), plus `record(…, status)` returning the id and `updateStatus`. Verification: compiles, unit + integration tests green.
- [x] Deposit failure rolls back
  > ✅ 2026-11-02 18:57 +0000 — catch-all removed; credit + ledger in one transaction. Verification: `aLedgerFailureRollsBackADeposit` (balance unchanged). Behaviour note: an unexpected database failure now surfaces as the framework default (HTTP 500) instead of the old 422 "Fail deposito at account" message; the generic error handler belongs to BE-05.
- [x] Outbox + relay + Flyway `V2`
  > ✅ 2026-11-02 18:57 +0000 — `V2__money_integrity.sql` (ledger `status` + CHECK, repair of legacy recharge rows + FK `transaction.account_id → account`, `outbox_event` table + partial index); `OutboxEvent`/`OutboxEventRepository`; `OutboxRelay` (`@Scheduled` every 1 s, at-least-once, keeps the row pending and records attempts/last_error on failure). Recharge writes debit + ledger (now with the account ID, F-08 fixed) + outbox row in one transaction. Verification: `OutboxRelayTest` (publishes payload as-is with the `__TypeId__` header; stays pending when Kafka is down), recharge integration test (row marked published after the broker acknowledged), `FailureInjectionTest` (no event without a commit). Topic and payload unchanged.
- [x] Pix reserve/confirm/compensate + failure-injection tests (MaiaBank 500, timeout, open breaker, DB failure)
  > ✅ 2026-11-02 18:57 +0000 — Reserve (atomic debit + `PENDING` ledger row, agency id now recorded) → commit → MaiaBank → confirm (`COMPLETED`) or compensate (credit back + `FAILED`), each in its own transaction; statement only counts `COMPLETED`. Feign connect/read timeouts 2 s/5 s; circuit-breaker slow-call threshold 100 ms → 2 s. Verification with the REAL Feign client against a stub MaiaBank (`PixMaiaBankResilienceTest`, 4 tests): success confirms; HTTP 500 and a read timeout (request returns in < 2.4 s) are compensated; after 6 failures the breaker is OPEN and the 7th Pix does NOT call MaiaBank (stub hit count stays 6) while every reservation is compensated — this also proves the `@CircuitBreaker` annotation on the Feign interface works (F-20 resolved). Plus mock-based failure tests in `FailureInjectionTest`.
- [x] Flip tests: exact-balance debit allowed
  > ✅ 2026-11-02 18:57 +0000 — BE-01 tests tagged F-10/F-08/F-09 flipped on purpose: exact-balance withdraw/Pix/recharge → 204 with balance 0.00; amount > balance → 422 with the same message and nothing written; recharge ledger `accountId` = account id; Pix ledger `agencyId` filled.

## Acceptance
- [x] Concurrency test fails before, passes after
  > ✅ 2026-11-02 18:57 +0000 — red run (`e63f6c5`): 2 failures; after the fix: `MoneyConcurrencyTest` 2/2 green.
- [x] Exact-balance debit succeeds; amount > balance → 422 same message
  > ✅ 2026-11-02 18:57 +0000 — `withdrawalOfTheExactBalanceIsAllowed`, `pixOfTheExactBalanceIsAllowed`, `rechargeOfTheExactBalanceIsAllowed` (204, balance 0.00); `…AboveTheBalance…` tests (422 "Operation not performed, there is not enough balance in the account.", nothing written).
- [x] After any injected failure balance and ledger are consistent
  > ✅ 2026-11-02 18:57 +0000 — `FailureInjectionTest` (6) + `PixMaiaBankResilienceTest` (4): see tasks above.
- [x] Recharge payload byte-identical to BE-01
  > ✅ 2026-11-02 18:57 +0000 — recharge test compares the raw Kafka value with the bytes of `new JsonSerializer<MobileRechargeRequestDTO>().serialize(topic, dto)` (the serializer the producer used before) and with the stored outbox payload: equal; the BE-01 field assertions also pass unchanged.
- [x] No package moved
  > ✅ 2026-11-02 18:57 +0000 — `git status` shows only modified/new files, no renames; no new packages (`OutboxRelay` in `config.kafka`, `OutboxEvent` in `entity`, `OutboxEventRepository` in `repository`).

## Minimal validation (required to close this spec)
- [x] Concurrency test (N parallel debits) passes: balance never < 0 and ledger sum = balance delta
  > ✅ 2026-11-02 18:57 +0000 — EXECUTED: `mvn -B -ntp verify` in `maven:3.9-eclipse-temurin-17` with Testcontainers PostgreSQL: **72 tests, 0 failures, BUILD SUCCESS**, 62.2% lines. `MoneyConcurrencyTest` 2/2.
- [x] A debit of exactly the full balance succeeds (204); amount > balance returns 422 with the same message
  > ✅ 2026-11-02 18:57 +0000 — EXECUTED (3 operations), see acceptance.
- [x] Injected MaiaBank failure (500/timeout) leaves balance and ledger consistent
  > ✅ 2026-11-02 18:57 +0000 — EXECUTED against a real HTTP stub: 500, 2.5 s delay with 500 ms read timeout, and open breaker (`PixMaiaBankResilienceTest` 4/4).
- [x] Recharge Kafka payload equals the BE-01 characterization
  > ✅ 2026-11-02 18:57 +0000 — EXECUTED: byte-for-byte equal (see acceptance).

## Stop and ask
- Any behaviour change beyond `>=` and the items above
- MaiaBank request body change

## Skills
java-persistence-performance (transactions-concurrency, Flyway), java-modern-engineering

## Result
Every money operation is now atomic and race-safe: one conditional UPDATE enforces `balance >= amount` (decision D-14), ledger + balance (+ outbox row for recharge) commit together, Pix is reserve → MaiaBank → confirm/compensate with timeouts and a verified circuit breaker, and the recharge event is published through a transactional outbox (same topic and payload bytes). Executed: concurrency tests (red before, green after), failure-injection tests, real-Feign resilience tests, flipped BE-01 characterizations; full suite 72/72, 62.2% lines. **Open:** PR + `api-ci` (owner).
**Accepted limitations / follow-ups:** (1) a crash between Pix reserve and confirm leaves a `PENDING` row with the account debited — no reconciliation job exists yet (candidate for BE-08); (2) the relay is at-least-once and has no `SKIP LOCKED`, so several instances could publish a row twice (no consumer exists, D-24); (3) `receiversPix()` is still an empty POST (waiting for the MaiaBank contract); (4) deposit's unexpected-failure response is now the default 500 until BE-05; (5) `AddAmountService` keeps its misleading class name (D-19).

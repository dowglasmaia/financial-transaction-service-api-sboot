# SPEC-ARCH-02 — Hexagonal Pragmática: the `transaction` feature and legacy cleanup (backend only)

**Status:** Draft (not authorized) · **Depends on:** ARCH-01 · **Parallel with:** SL-00 (frontend only) · before SL-01
**Branch:** `spec/arch-02-hexagonal-transaction` · **Track:** Backend only · **Behaviour:** frozen (D-41)
**Read first:** `arch-01-hexagonal-pilot.md` (mapping table, constraints), `../memory/produto.md` (the money rules must not change), `../memory/contexto-tecnico.md` ("As built in BE-03": atomic debit, Pix reserve → MaiaBank → confirm/compensate, outbox), the skill `java-spring-create-module` (mode `migrate`). Protocol: `../shared/como-executar.md`.

## Objective
Migrate the rest of the backend — statement, deposit, withdrawal, mobile recharge and Pix — into the model and **delete the legacy packages**, so the architecture is enforced strictly (`ArchitectureTest` with an empty legacy list). After this spec every new backend capability (SL-01…SL-06) is created with the skill in its final place.

## Constraints
The ARCH-01 constraints stay: HTTP contract, messages, Flyway schema, Kafka topic/payload bytes, test count, move-vs-behaviour commits. Additionally for money:
- the atomic conditional debit stays one atomic SQL update (port method `tryDebit`);
- Pix keeps **reserve → commit → MaiaBank → confirm/compensate** with the remote call **outside** any database transaction;
- recharge keeps **debit + ledger row + outbox row in one transaction**; the relay keeps at-least-once publishing of identical bytes and header `__TypeId__`.

## Scope
**Feature `transaction`** (the ledger and the money operations; the single generated `TransactionsApi` controller lives here):
- `transaction.domain`: `Transaction` (ledger entry with its `PENDING → COMPLETED/FAILED` transitions as methods), `TransactionType`, `TransactionStatus`; pure Java, flat.
- `transaction.application.port`: `TransactionRepository`, `PixGateway`, `RechargeEventPublisher` (interfaces only).
- `transaction.application.usecase`: **one class per operation** (layout v2, D-50): `MakeDepositUseCase`, `MakeAtmWithdrawalUseCase`, `RequestMobileRechargeUseCase`, `MakePixTransferUseCase` (keeps its own reserve → external call → confirm/compensate model with `TransactionTemplate`), `GetStatementUseCase`, `GetTransactionByIdUseCase`, plus the internal `SaveTransactionUseCase`/`RecordTransactionUseCase`/`UpdateTransactionStatusUseCase` the money flows share — names and the final list confirmed in the mapping task.
- `transaction.api`: `TransactionController implements TransactionsApi`, shape validation, `TransactionMapper` (kept as is if it is used), generated DTOs never leave this package.
- `transaction.infrastructure.persistence`: `TransactionEntity`, `TransactionJpaRepository`, adapter (statement queries keep their aggregates and ordering).
- `transaction.infrastructure.client`: `MaiaBankFeignClient` + `MaiaBankPixGatewayAdapter` (timeouts, circuit breaker, fallback behaviour unchanged).
- `transaction.infrastructure.messaging`: `OutboxEventEntity`, its Spring Data repository, `OutboxRelay`, Kafka producer/admin config, `OutboxRechargeEventPublisher` implementing the port.
- Rules that live in the domain: Pix key formats (CPF/e-mail/phone validators move to `transaction.domain`, **same classes and behaviour**, characterization test untouched), `amount > 0`, the statement date conversion (`DateConverter`) stays as is, only relocated.

**Legacy cleanup:** delete `controllers`, `services`, `entity`, `repository`, `config` (what remains), `exeptions`, `utils` once empty; `exeptions.*` → `shared.error.*` (typo fix in its **own** commit); dead classes found unused (`TransactionRequestDTO`, `AccountModel`) are only moved/ignored here and removed by BE-08 (decide in the mapping).
**Cross-feature relations:** `Transaction ↔ Agency/Account` become plain id columns in `TransactionEntity` (the DB foreign key stays); anything that needs the related object goes through the `account.application.usecase` classes (D6). Owner approves this in task 1.

**Out of scope:** new endpoints, contract changes, the `Idempotency-Key` (SL-03), removal of dead code (BE-08), Boot/Java upgrades (BE-07).

## Tasks
- [ ] Baseline (after ARCH-01 is merged): `./mvnw -B verify` green, count recorded
- [ ] Confirm the mapping for the transaction side (services split by D4, cross-feature relations, exception names, dead classes); owner approves (HUMAN gate)
- [ ] Domain tests first for the rules found in the services (red) → `transaction.domain`: `Transaction` transitions, pure validators/value rules
- [ ] `transaction.infrastructure.persistence` + adapter; statement/aggregate/detail queries unchanged; adapter tests
- [ ] `transaction.application.usecase`: each service operation becomes a `<Verb><Noun>UseCase`, interfaces and `*Impl` removed, `@Transactional` boundaries identical; `MoneyConcurrencyTest`, `FailureInjectionTest`, `PixMaiaBankResilienceTest` green and unchanged
- [ ] `transaction.infrastructure.client` (MaiaBank) and `transaction.infrastructure.messaging` (outbox/relay/Kafka) behind their ports; `OutboxRelayTest` and the recharge payload test green and unchanged
- [ ] `transaction.api`: controller + mapper moved; the four characterization suites and `TransactionControllerTest` green
- [ ] Exceptions → `shared.error` (own commit), tests follow
- [ ] Delete the empty legacy packages; set `LEGACY_PACKAGES` to empty; checker without `--legacy` and with `--strict` → 0 violations
- [ ] Docs: rewrite the *Code map* and *As built* sections of `memory/contexto-tecnico.md`; update README/`como-executar` run commands if any changed; note what BE-08 still owns
- [ ] Smoke: `docker compose up`, `/actuator/health` UP, one authenticated statement + one withdrawal, Kafka recharge event observed in Kafdrop (state what was run)

## Acceptance
- [ ] No legacy package remains; strict checker 0 violations; `ArchitectureTest` green with an empty legacy list
- [ ] Test count ≥ baseline, none deleted; all characterization/integrity/ownership/auth tests pass unchanged apart from imports and package names
- [ ] `git diff main -- api-financial-transaction-service/src/main/resources` is empty (contract, Flyway, config unchanged); Kafka payload bytes identical; Hibernate `validate` green
- [ ] Moves are renames (`git diff --name-status main`); no commit mixes a move with a behaviour edit
- [ ] Money rules unchanged: atomic debit, Pix remote call outside transactions, recharge atomicity

## Minimal validation (required to close this spec)
- [ ] `./mvnw -B verify` green; state the test count versus the baseline
- [ ] `node ~/.claude/skills/java-spring-create-module/scripts/check-architecture.mjs --src api-financial-transaction-service/src/main/java --base-package com.dowglasmaia.financialtransaction --strict` → 0 violations
- [ ] `ArchitectureTest` green in `mvn verify`; Kafka payload, concurrency and failure-injection tests green and unchanged
- [ ] Compose smoke (health, one read, one write, Kafka event) reported as executed or not
- [ ] `api-ci` green on the PR

## Stop and ask
- Mapping/D4 decisions that change a public behaviour; any need to alter a transaction boundary, the schema, the contract or the Kafka payload; a class that cannot be classified; renaming public API classes beyond the agreed list

## Skills
java-spring-create-module (mode `migrate`), java-persistence-performance (transactions-concurrency), java-modern-engineering

## Result
_Filled on completion: result, checks run, accepted limitations._

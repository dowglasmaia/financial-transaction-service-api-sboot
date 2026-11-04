# SPEC-ARCH-02 — Hexagonal Pragmática: the `transaction` feature and legacy cleanup (backend only)

**Status:** Implemented on branch `spec/arch-02-hexagonal-transaction` (pushed, no PR yet); waiting for `api-ci` on the PR and the owner merge · **Depends on:** ARCH-01 (merged, PR #17) · runs before SL-02
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
- [x] Baseline (after ARCH-01 is merged): `./mvnw -B verify` green, count recorded
  - Evidence: `mvn clean verify` on the new branch (main with ARCH-01, PR #17): BUILD SUCCESS, **156 tests**, 0 failures, 1 skipped (JDK 21; Maven 3.9.11 from `~/.m2/wrapper` because `./mvnw` is broken; `MAVEN_OPTS` with the Windows trust store).
- [x] Confirm the mapping for the transaction side (use cases, cross-feature relations, exception names, dead classes); owner approves (HUMAN gate)
  - Evidence: mapping and decisions A-H presented in chat; owner replied "Ajustar antes", then approved A-H and asked to create value objects where they fit (done: `PixKey`, `PixTransfer`, `MobileRecharge`, `Statement`).
- [x] Domain tests first for the rules found in the services (red) -> `transaction.domain`: `Transaction`, value objects, pure validators/value rules
  - Evidence: `TransactionTest`, `PixKeyTest`, `MobileRechargeTest`, `StatementTest` (9 cases) red (classes missing) then green (7a6de04); enums, validators and `DateConverter` moved with `git mv` (8e716ac).
- [x] `transaction.infrastructure.persistence` + adapter; statement/aggregate/detail queries unchanged; adapter tests
  - Evidence: entity/repo moved (a133cae); `TransactionRepositoryAdapterTest` (4 cases: save, own-account lookup, statement filter by status/account/period, single-statement `updateStatus`) red then green; `StatementCharacterizationTest` unchanged and green.
- [x] `transaction.application.usecase`: each service operation becomes a `<Verb><Noun>UseCase`, interfaces and `*Impl` removed, `@Transactional` boundaries identical; `MoneyConcurrencyTest`, `FailureInjectionTest`, `PixMaiaBankResilienceTest` green and unchanged
  - Evidence: 9 use cases + 15 unit tests (red: classes missing, then green; the 3 cases of `AtmWithdrawalServiceImplTest` live on in `MakeAtmWithdrawalUseCaseTest`); the five services and their interfaces removed (9b2a1d6); full suite 184 green with the three integrity suites unchanged apart from imports/type names. Boundaries: deposit/withdrawal/recharge `@Transactional` on the use case; Pix `execute` has no `@Transactional` and uses `TransactionTemplate`; statement and by-id have none (as before).
- [x] `transaction.infrastructure.client` (MaiaBank) and `transaction.infrastructure.messaging` (outbox/relay/Kafka) behind their ports; `OutboxRelayTest` and the recharge payload test green and unchanged
  - Evidence: `PixGateway` + `MaiaBankPixGatewayAdapter` (2 tests); `RechargeEventPublisher` + `OutboxRechargeEventPublisher` (test pins the exact bytes `{"phoneNumber":998877665,"areaCode":11,"carrier":"VIVO","amount":50.00}`, topic and `__TypeId__` type); `OutboxRelayTest` and `PixRechargeCharacterizationTest` green; Kafdrop shows the two events with that payload after the Docker smoke. Deviation: the outbox entity and its Spring Data repository sit in `infrastructure.persistence` (not `messaging`) to satisfy rule A11 (every `@Entity` of a rich feature in `persistence`).
- [x] `transaction.api`: controller + mapper moved; the four characterization suites and `TransactionControllerTest` green
  - Evidence: `TransactionController` (calls the use cases, maps DTOs <-> domain values), `ValidateBodyRequest`, `TransactionMapper`, `TransactionRequestDTO` moved with `git mv` (a6ec1fd); the four characterization suites green in the 184-test run.
- [x] Exceptions -> `shared.error` (own commit), tests follow
  - Evidence: move-only commit, then the typo-fix commit (`ForbiddenException`, `ObjectNotFoundException`, `UnprocessableEntityException`); messages and statuses unchanged.
- [x] Delete the empty legacy packages; set `LEGACY_PACKAGES` to empty; checker without `--legacy` and with `--strict` -> 0 violations
  - Evidence: no file left in `controllers/services/entity/repository/config/exeptions/utils`; `ArchitectureTest` regenerated with `Set.of()` (15/15); strict checker: "checked 74 files: 0 violation(s), 0 warning(s)".
- [x] Docs: rewrite the *Code map* and *As built* sections of `memory/contexto-tecnico.md`; update README/`como-executar` run commands if any changed; note what BE-08 still owns
  - Evidence: code map, BE-03/BE-04 as-built lines, D-51..D-53 in `contexto-tecnico.md`; API README checker command now strict.
- [x] Smoke: `docker compose up`, `/actuator/health` UP, one authenticated statement + one withdrawal, Kafka recharge event observed in Kafdrop (state what was run)
  - Evidence: `docker compose -f docker-compose.yml -f docker-compose.e2e.yml --profile api --profile e2e up -d --build financial-transaction-api mock-idp`; health `{"status":"UP"}`; with a token from the mock IdP (user 1): `GET /accounts/me` 200, `GET /transactions?initial-date&final-date` 200, withdrawal 204, deposit 204, recharge 204, withdrawal above the balance 422 with the known message, empty Pix body 422 "Pix key is required"; the valid Pix returned 422 "Fail send Pix." because the local MaiaBank simulator rejected the call in this environment, and the compensation worked (balance after all operations = 5000 - 10 + 5 - 15 = 4980.00); Kafdrop (http://localhost:19000) lists the recharge events with the expected payload.

## Acceptance
- [x] No legacy package remains; strict checker 0 violations; `ArchitectureTest` green with an empty legacy list
  - Evidence: see the cleanup task.
- [x] Test count >= baseline, none deleted; all characterization/integrity/ownership/auth tests pass unchanged apart from imports and package names
  - Evidence: **184 tests** (baseline 156), 0 failures, 1 skipped (same as the baseline). 28 net new: 31 added (domain 9, adapter 4, publisher 1, gateway 2, use cases 15) minus the 3 cases of `AtmWithdrawalServiceImplTest`, which moved into `MakeAtmWithdrawalUseCaseTest`. Edits beyond imports/package names, all listed in commit messages: `TransactionControllerTest` (mocks the use cases), `TransactionMapperTest` (domain entries), `AtmWithdrawalServiceImplTest` -> `MakeAtmWithdrawalUseCaseTest`.
- [x] `git diff main -- api-financial-transaction-service/src/main/resources` is empty (contract, Flyway, config unchanged); Kafka payload bytes identical; Hibernate `validate` green
  - Evidence: 0 diff lines; payload pinned by `OutboxRechargeEventPublisherTest` and `PixRechargeCharacterizationTest`; `FlywaySchemaTest` 12/12 (Hibernate `validate`).
- [x] Moves are renames (`git diff --name-status main`); no commit mixes a move with a behaviour edit
  - Evidence: `git diff -M --name-status main` shows 22 renames (R), 21 additions, 10 deletions, 6 modifications under `src/main`; each move commit is move + package/import/type rename only (see the commit messages).
- [x] Money rules unchanged: atomic debit, Pix remote call outside transactions, recharge atomicity
  - Evidence: `DebitAccountUseCase`/`AccountRepository.tryDebit` untouched; `MakePixTransferUseCase` keeps reserve -> gateway -> confirm/compensate with `TransactionTemplate` and no `@Transactional` on `execute`; `RequestMobileRechargeUseCase` is one `@Transactional` unit (debit, ledger, outbox) - `FailureInjectionTest` (6) and `MoneyConcurrencyTest` green.

## Minimal validation (required to close this spec)
- [x] `./mvnw -B verify` green; state the test count versus the baseline
  - Evidence: `mvn clean verify` BUILD SUCCESS, 184 tests vs 156 baseline (JDK 21, see the baseline note for the Maven wrapper).
- [x] `node ~/.claude/skills/java-spring-create-module/scripts/check-architecture.mjs --src api-financial-transaction-service/src/main/java --base-package com.dowglasmaia.financialtransaction --strict` -> 0 violations
  - Evidence: "checked 74 files: 0 violation(s), 0 warning(s)".
- [x] `ArchitectureTest` green in `mvn verify`; Kafka payload, concurrency and failure-injection tests green and unchanged
  - Evidence: 15/15 in the 184-test run; `OutboxRelayTest`, `MoneyConcurrencyTest`, `FailureInjectionTest` green.
- [x] Compose smoke (health, one read, one write, Kafka event) reported as executed or not
  - Evidence: executed, see the smoke task.
- [ ] `api-ci` green on the PR

## Stop and ask
- Mapping/D4 decisions that change a public behaviour; any need to alter a transaction boundary, the schema, the contract or the Kafka payload; a class that cannot be classified; renaming public API classes beyond the agreed list

## Skills
java-spring-create-module (mode `migrate`), java-persistence-performance (transactions-concurrency), java-modern-engineering

## Result
**Implemented 2026-11-03** on `spec/arch-02-hexagonal-transaction` (pushed, no PR). The whole backend now follows the model: `account`, `transaction` and `shared`; **no legacy package remains** and `ArchitectureTest` runs with an empty legacy list (strict checker: 74 files, 0 violations).

**What the `transaction` feature looks like:** `domain` (`Transaction`, `TransactionType`, `TransactionStatus`, the value objects `PixKey`/`PixKeyType`/`PixTransfer`/`MobileRecharge`/`Statement`, the CPF/e-mail/phone validators and `DateConverter`) · `application.port` (`TransactionRepository`, `PixGateway`, `RechargeEventPublisher`) · `application.usecase` (`MakeDepositUseCase`, `MakeAtmWithdrawalUseCase`, `RequestMobileRechargeUseCase`, `MakePixTransferUseCase`, `GetStatementUseCase`, `GetTransactionByIdUseCase`, `RecordTransactionUseCase`, `SaveTransactionUseCase`, `UpdateTransactionStatusUseCase`) · `api` (controller, body validators, mapper) · `infrastructure` (`persistence`: ledger and outbox entities, Spring Data repositories, adapter; `messaging`: Kafka config, `OutboxRelay`, `OutboxRechargeEventPublisher`; `client`: `MaiaBankClient`, `MaiaBankPixGatewayAdapter`). `shared.error` holds the three exception classes (typo fixed).

**Executed:** `mvn clean verify` (184 tests, BUILD SUCCESS); the strict checker (0 violations); `git diff main -- resources` (empty); the API rebuilt in Docker with an authenticated read, writes, 422 cases and the recharge event seen in Kafdrop; red-then-green runs of every new test.
**Only written / not run:** `api-ci` on the PR; the standalone checker is still not a CI step (only `ArchitectureTest` runs in `mvn verify`); coverage percentage not recorded; the happy path of a real Pix against the MaiaBank simulator (it rejected the call in this environment; the happy path is covered by `PixRechargeCharacterizationTest` and `MakePixTransferUseCaseTest`).

**Deviations from the spec text (approved or explained):**
- The use cases never see the generated DTOs: the controller maps them to the domain values; the Pix error message keeps the lower-case key type through `PixKeyType.label()`.
- The outbox entity and repository live in `infrastructure.persistence` instead of `messaging` (rule A11).
- `AccountModel` (unused) moved to `infrastructure.client` and `TransactionRequestDTO` (unused) to `api`; both are dead code left for BE-08.
- The statement totals (income/outgoing) stay in `TransactionMapper` (`api`) unchanged, because `TransactionMapperTest` pins them; moving that rule into the domain is a BE-08 candidate.
- Test edits beyond imports/package names: `TransactionControllerTest`, `TransactionMapperTest`, `AtmWithdrawalServiceImplTest` -> `MakeAtmWithdrawalUseCaseTest`; `AccountMeTest` moved to `account.api` (package only).

**What BE-08 still owns:** `Money` and `Account` invariants, the dead classes above, the unused Kafka beans (`JsonProducerFactory`, `mobileRechargeRequesKafkaTemplate`), the `httpStatus` field of the exception classes (never read), log masking, correlation id, the standalone checker as a CI step.

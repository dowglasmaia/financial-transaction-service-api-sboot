# SPEC-BE-01 — Safety net: CI + characterization tests

**Status:** In progress (approved by the owner 2026-11-02) · **Depends on:** none · **Parallel with:** FE-01
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Pin today's behaviour (including known quirks) with tests and run them in CI, before anything changes.

## Scope
- Extend `.github/workflows/api-ci.yml` (created by MONO-01): add JaCoCo report and Testcontainers support (Docker is available on GitHub runners)
- Testcontainers PostgreSQL, WireMock (MaiaBank), EmbeddedKafka; shared integration-test base
- Characterization tests for every rule in `memory/produto.md`; quirks tagged `// characterizes F-xx` (strict `>` balance, unscoped statement, end-of-day dates, validator NPEs, recharge `accountId`)
- Replace the vacuous `AtmWithdrawalServiceImplTest` assertions

**Out of scope:** Any change under `src/main`

## Tasks
- [ ] Extend `api-ci.yml` (JaCoCo; keep path filter and `working-directory`)
  > ⏳ 2026-11-02 16:45 +0000 — Partial: JaCoCo plugin added to `pom.xml` (report generated locally at `target/site/jacoco`); the existing workflow already uploads that folder, so it needed no change. Falta: first real run on GitHub Actions (needs push). Próxima ação: open the PR.
- [x] Add test deps + base class
  > ✅ 2026-11-02 16:45 +0000 — `org.testcontainers:junit-jupiter` and `postgresql` (test scope, versions managed by Boot 3.2.5), `AbstractIntegrationTest` (PostgreSQL container, EmbeddedKafka, `@MockBean MaiaBankClient`). Verification: `mvn -B -ntp verify` in container `maven:3.9-eclipse-temurin-17` (Docker socket + Testcontainers PostgreSQL 14.12): 45 tests, 0 failures/errors/skipped, BUILD SUCCESS. Desvio: WireMock replaced by a mocked Feign client (no new dependency; HTTP/circuit-breaker behaviour is BE-03's concern).
- [x] Tests: deposit, withdraw (card + non-card), recharge (+Kafka payload)
  > ✅ 2026-11-02 16:45 +0000 — `DepositWithdrawCharacterizationTest` (11) and `PixRechargeCharacterizationTest` (10, includes the Kafka JSON payload read from the embedded broker). Verification: same run, all passed.
- [x] Tests: Pix ok / bad key / no balance / MaiaBank failure restores balance
  > ✅ 2026-11-02 16:45 +0000 — in `PixRechargeCharacterizationTest`; balance restore after MaiaBank failure (BR-14) asserted. Verification: same run.
- [x] Tests: statement + by-id + error shapes
  > ✅ 2026-11-02 16:45 +0000 — `StatementCharacterizationTest` (9), `ValidatorsCharacterizationTest` (8), `TransactionMapperTest` (2), rewritten `AtmWithdrawalServiceImplTest` (2, real verifications instead of the vacuous assertions), existing `TransactionControllerTest` (3). Verification: same run.
- [ ] JaCoCo baseline in PR
  > ⏳ 2026-11-02 16:45 +0000 — Parcial: baseline measured locally = **61.1% lines (538/880), 42.1% instructions** (all classes incl. config/Kafka/generated-adjacent code). Falta: paste it in the PR description. Próxima ação: open the PR.

## Acceptance
- [ ] `./mvnw -B verify` green locally and in CI on Java 17
  > ⏳ 2026-11-02 16:45 +0000 — Local: green (`mvn -B -ntp verify` in container `maven:3.9-eclipse-temurin-17` (Docker socket + Testcontainers PostgreSQL 14.12): 45 tests, 0 failures/errors/skipped, BUILD SUCCESS); the Maven wrapper itself was not used (no JDK on the host), plain Maven 3.9 in the container. CI: not run yet.
- [x] Every rule in produto.md covered; each defect has a tagged test
  > ✅ 2026-11-02 16:45 +0000 — rules 1–9 of 'Rules that must be preserved' each have at least one assertion; defects F-04, F-05, F-07, F-08, F-09, F-10, F-11 are tagged `characterizes F-xx` (F-02/F-03 are not characterized here: they are race/atomicity defects proven by BE-03's failing concurrency test). Verification: review of the test classes against produto.md.
- [x] `git diff -- src/main` is empty
  > ✅ 2026-11-02 16:45 +0000 — `git status --short -- src/main` shows only the staged monorepo moves, no modifications. Desvio: `git diff --stat` errored on one long Windows path (git 'Filename too long'); `git status` used instead.

## Minimal validation (required to close this spec)
- [x] `cd api-financial-transaction-service && ./mvnw -B verify` is green locally
  > ✅ 2026-11-02 16:45 +0000 — run through Docker (no JDK on the host): `mvn -B -ntp verify` in container `maven:3.9-eclipse-temurin-17` (Docker socket + Testcontainers PostgreSQL 14.12): 45 tests, 0 failures/errors/skipped, BUILD SUCCESS. Note: Testcontainers needed `api.version=1.44` in `~/.docker-java.properties` because the local Docker Engine (29.x) rejects the old API version used by Testcontainers 1.19.7; GitHub runners (older Engine) should not need it.
- [ ] `api-ci` is green on the PR
  > ⛔ Not executed: needs the MONO-01 move committed and a push/PR. Para desbloquear: commit + push.
- [x] `git diff -- api-financial-transaction-service/src/main` is empty
  > ✅ 2026-11-02 16:45 +0000 — see acceptance item above.
- [x] Each rule in `memory/produto.md` ('Rules that must be preserved') maps to at least one test
  > ✅ 2026-11-02 16:45 +0000 — see acceptance item above.

## Stop and ask
- New test-scope dependencies

## Skills
java-modern-engineering, java-persistence-performance (Testcontainers, no H2)

## Result (in progress)
Suite of 45 tests green locally (PostgreSQL via Testcontainers, embedded Kafka); coverage baseline 61.1% lines. Open: first GitHub Actions run (`api-ci`), baseline in the PR. Findings from the work: (1) the `validatorPixRequest/…` NPE (F-11) is **unreachable over HTTP** (the body is required, so Spring rejects null bodies with 400 first) — only direct calls throw; (2) `GET /transactions` without dates throws an NPE that MockMvc surfaces as a servlet exception (a 500 on a running server); (3) Kafka embedded-broker shutdown logs checkpoint errors at JVM exit — benign.

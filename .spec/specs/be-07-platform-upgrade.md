# SPEC-BE-07 — Platform upgrade: Boot 3.5.x, Java 21, Java 25

**Status:** Implemented on `spec/be-07-08-backend-close` together with BE-08, awaiting the owner's PR, review and green pipelines (owner decisions 2026-11-04: Eclipse Temurin images, Spring Boot 3.5.16) · **Depends on:** SL-06 (replaces BE-06) · **Parallel with:** none
> Note 2026-11-03: the local build was moved to Java 21 with Lombok `1.18.42` (uncommitted when written); the pinned `1.18.24` made `javac` 21+ fail (*JCImport qualid*). Step 2 below is therefore partly done — commit it as its own change, rerun the suite, then continue.
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Modernize the runtime in small reversible steps; behaviour must not change.

## Scope
- Step 1 (still Java 17): compiler plugin ≥ 3.13 with `release`, remove unused MapStruct/protobuf/javax/Sleuth BOM, Boot → latest 3.5.x + matching Spring Cloud, Lombok upgrade
- Step 2: release 21 (CI + Docker); Step 3: release 25 (JDK 25 images); remove `-noverify`; `jdeps`/`jdeprscan` review
- Adopt only justified features (records for new value types, pattern-matching switch); no virtual threads without measurements; no Boot 4

**Out of scope:** Boot 4; Broad syntax modernization

## Tasks
- [x] Baseline `dependency:tree` + Actuator/Prometheus output
> ✅ 2026-11-04 — `dependency:tree` saved before the change (not committed); the metric names are frozen by the new `MetricNamesBaselineTest` (recorded on Boot 3.2.5 / Java 17, green there). Verification: `./mvnw -B -Dtest=MetricNamesBaselineTest test` → 1 passed.
- [x] Step 1 as separate commits (hygiene, then Boot)
> ✅ 2026-11-04 — hygiene (compiler plugin 3.13 with `release`; removed protobuf, javax validation/annotation/servlet, swagger-annotations, MapStruct and its processors, OpenTelemetry API and the Sleuth/OpenTelemetry BOMs) then Boot 3.5.16 + Spring Cloud 2025.0.3 (Flyway 11 needed `flyway-database-postgresql`). Verification: `./mvnw -B verify` after each → 372 tests, 0 failures, 1 skipped.
- [x] Step 2 Java 21
> ✅ 2026-11-04 — `java.version` 21 and `api-ci` 21. Verification: `./mvnw -B verify` (JDK 21) → 372, 0 failures.
- [x] Step 3 Java 25 + Dockerfile
> ✅ 2026-11-04 — `java.version` 25, `api-ci` 25, JaCoCo 0.8.16 and ArchUnit 1.5.1 (the old ones do not read Java 25 classes), Dockerfile on `maven:3.9-eclipse-temurin-25` → `eclipse-temurin:25-jre-alpine`, `-noverify` removed. Verification: `./mvnw -B verify` (JDK 25) → 372 then 410 tests, 0 failures; `docker exec … java -version` → `Temurin-25.0.5+8`.
- [x] `jdeprscan --release 25` and triage
> ✅ 2026-11-04 — `jdeprscan --release 25 --class-path <deps> target/classes` → 0 deprecated API uses; `jdeps` → `java.base,java.sql`. The only deprecation left in the build was `@MockBean`, replaced by `@MockitoBean` (BE-08 commit).
- [x] Compose smoke test
> ✅ 2026-11-04 — `docker compose -f docker-compose.yml -f docker-compose.e2e.yml --profile api --profile e2e up -d --build financial-transaction-api` → container `healthy`, `/actuator/health` `{"status":"UP"}`; the full Playwright suite then passed 114/114 against it.

## Acceptance
- [x] Full suite green on 17/Boot 3.5, then 21, then 25
> ✅ 372 tests green on release 17 with Boot 3.5.16, then on 21, then on 25.
- [x] `java -version` is 25 in the runtime image
> ✅ `openjdk version "25.0.5"` / `Temurin-25.0.5+8-LTS` inside the runtime image.
- [x] Metric names unchanged
> ✅ `MetricNamesBaselineTest` (15 names: http.server.requests, jvm.*, hikaricp.*, resilience4j.circuitbreaker.*, spring.kafka.template, …) green on Boot 3.5.16 / Java 25.

## Minimal validation (required to close this spec)
- [x] Suite green after each step
> ✅ see the step lines above (372 → 410 tests).
- [x] `java -version` inside the runtime image prints 25
> ✅ prints 25.
- [x] `docker compose up --build` starts the API and `/actuator/health` is UP
> ✅ `/actuator/health` is UP (now on the management port 8090 inside the container; see BE-08).
- [x] Metric names unchanged vs baseline
> ✅ `MetricNamesBaselineTest` (15 names: http.server.requests, jvm.*, hikaricp.*, resilience4j.circuitbreaker.*, spring.kafka.template, …) green on Boot 3.5.16 / Java 25.

## Stop and ask
- Exact Boot version
- JDK vendor/images

## Skills
java-modern-engineering (version matrix, migration)

## Result
_Filled on completion: result, checks run, accepted limitations._

## Result (2026-11-04)
Platform: Spring Boot **3.5.16**, Spring Cloud **2025.0.3**, **Java 25** (Eclipse Temurin images and `actions/setup-java`), Flyway 11, JaCoCo 0.8.16, ArchUnit 1.5.1. 410 backend tests green on JDK 25 (372 before BE-08). Accepted notes: the local shell default JDK must be 21 or 25 (the old Mockito of Boot 3.2 broke on 25; Boot 3.5 does not); `-noverify` is gone; no virtual threads and no syntax modernization were adopted (out of scope, no measurements). **Not run:** `api-ci` on a pull request.

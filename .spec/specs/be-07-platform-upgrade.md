# SPEC-BE-07 — Platform upgrade: Boot 3.5.x, Java 21, Java 25

**Status:** Draft (not authorized) · **Depends on:** BE-06 · **Parallel with:** none
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Modernize the runtime in small reversible steps; behaviour must not change.

## Scope
- Step 1 (still Java 17): compiler plugin ≥ 3.13 with `release`, remove unused MapStruct/protobuf/javax/Sleuth BOM, Boot → latest 3.5.x + matching Spring Cloud, Lombok upgrade
- Step 2: release 21 (CI + Docker); Step 3: release 25 (JDK 25 images); remove `-noverify`; `jdeps`/`jdeprscan` review
- Adopt only justified features (records for new value types, pattern-matching switch); no virtual threads without measurements; no Boot 4

**Out of scope:** Boot 4; Broad syntax modernization

## Tasks
- [ ] Baseline `dependency:tree` + Actuator/Prometheus output
- [ ] Step 1 as separate commits (hygiene, then Boot)
- [ ] Step 2 Java 21
- [ ] Step 3 Java 25 + Dockerfile
- [ ] `jdeprscan --release 25` and triage
- [ ] Compose smoke test

## Acceptance
- [ ] Full suite green on 17/Boot 3.5, then 21, then 25
- [ ] `java -version` is 25 in the runtime image
- [ ] Metric names unchanged

## Minimal validation (required to close this spec)
- [ ] Suite green after each step
- [ ] `java -version` inside the runtime image prints 25
- [ ] `docker compose up --build` starts the API and `/actuator/health` is UP
- [ ] Metric names unchanged vs baseline

## Stop and ask
- Exact Boot version
- JDK vendor/images

## Skills
java-modern-engineering (version matrix, migration)

## Result
_Filled on completion: result, checks run, accepted limitations._

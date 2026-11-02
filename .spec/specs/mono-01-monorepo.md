# SPEC-MONO-01 — Monorepo restructure and isolated CI

**Status:** Done (PR #1 merged 2026-11-02) · **Depends on:** none · **Parallel with:** none (runs first)
**Read first:** `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md`.

## Objective
Turn the repository into a monorepo with two independent applications — backend `api-financial-transaction-service` and frontend `app-financial-transaction-service` — each with its own isolated, path-filtered CI pipeline, keeping `prototype/` as the frontend's visual reference.

## Scope
- Move the Maven project into `api-financial-transaction-service/` with `git mv` (history preserved); **no source, package or build-file content changed**.
- Root keeps: `.spec/`, `prototype/`, `.github/workflows/`, `docker-compose.yml` (build context updated), a monorepo `README.md` and `.gitignore`.
- Two workflows: `api-ci.yml` (Java/Maven) and `app-ci.yml` (Node/Angular; skipped until the project exists).
- The Angular project is **not** created here: spec FE-02 creates `app-financial-transaction-service/` with the scaffold skill (which refuses to write into an existing directory).

**Out of scope:** any behaviour change, dependency change, Java/Boot upgrade, test additions (BE-01).

## Tasks
- [x] Move backend files (`.mvn`, `mvnw`, `mvnw.cmd`, `pom.xml`, `src`, `Dockerfile`, `README.md`, `.gitignore`) into `api-financial-transaction-service/`.
  > ✅ 2026-11-02 16:00 +0000 — `git mv`; `git status` shows renames (R) only. Verification: file listing of both folders. Build not run (no JDK on this machine).
- [x] Update `docker-compose.yml` build context to `./api-financial-transaction-service`.
  > ✅ 2026-11-02 16:00 +0000 — YAML parses; `docker compose build` **not executed** (Docker not run).
- [x] Add root `README.md` and `.gitignore` for the monorepo.
  > ✅ 2026-11-02 16:00 +0000 — files created.
- [x] Add `.github/workflows/api-ci.yml` (path filter `api-financial-transaction-service/**`, `working-directory`, JDK 17, `./mvnw -B -ntp verify`, upload reports).
  > ✅ 2026-11-02 16:00 +0000 — YAML parses. **Not run on GitHub yet.**
- [x] Add `.github/workflows/app-ci.yml` (path filter `app-financial-transaction-service/**`, Node 22, `npm ci` + `npm run build`, skipped while `package.json` is absent).
  > ✅ 2026-11-02 16:00 +0000 — YAML parses. **Not run on GitHub yet.**
- [x] Push to GitHub and confirm `api-ci` runs for API changes (done on PR #1). Path-filter isolation in the other direction remains unproven (PR #1 touched both folders, both ran).
  > ✅ 2026-11-02 — PR #1: `api-ci` and `app-ci` both triggered (the PR changed API and `.spec/`/root files); `app-ci` skipped its steps because the Angular project does not exist yet.
- [x] Confirm `api-ci` result.
  > ✅ 2026-11-02 — green on 062cf0b after the `mvnw` exec-bit fix (BE-01).
- [x] Fix the IDE Maven reference (IntelliJ `.idea/` pointed to the old root `pom.xml`).
  > ✅ 2026-11-02 — `.idea/misc.xml` `originalFiles` → `$PROJECT_DIR$/api-financial-transaction-service/pom.xml`; `.idea/encodings.xml` source path updated. `.idea/` is git-ignored (local only). Verification: file contents read back. The IDE must still *Reload All Maven Projects* (human).
- [x] Docker references: `Dockerfile` paths are relative to its build context (unchanged and correct); compose context points at the API folder; added `api-financial-transaction-service/.dockerignore`; API README run command now says to run from the repo root.
  > ✅ 2026-11-02 — `docker compose build financial-transaction-api` executed locally 2026-11-02: exit 0, image built from the new folder (`mvn clean install` inside the build stage reported BUILD SUCCESS).
- [ ] Human: in IntelliJ run *Maven → Reload All Maven Projects* (or *Link Maven Project* → select `api-financial-transaction-service/pom.xml`) and set the project SDK to JDK 17 until BE-07 (see note below).

## Acceptance
- [x] `git diff --stat -M` shows only renames for the API files (no content edits to moved files).
- [ ] Both workflows trigger independently on GitHub (path filters verified) — to be proven by the first PR that touches only one app folder (FE-02 or BE-02).
- [x] `docker compose build` builds the API image from the new folder (local run, exit 0). Full `up` with PostgreSQL/Kafka not run.

## Notes
- The IDE project SDK was `corretto-25` (language level 25). The build still targets Java 17 with Lombok 1.18.24, which does not support JDK 25 — compile errors in the IDE are expected until the SDK is JDK 17 (or until BE-07 upgrades Lombok/Java).
- If IntelliJ keeps losing the project after moves, open the folder `api-financial-transaction-service/` as its own project, or open the repo root and link the pom as above.

## Minimal validation (required to close this spec)
- [ ] `api-ci` runs for an API-only change and does NOT run for an app-only change (and vice versa)
- [ ] `docker compose build financial-transaction-api` exits 0 (already done once locally)

## Stop and ask
- Any content change to moved files; any change to the Angular project name/location.

## Skills
`java-modern-engineering` (build layout check), `angular-frontend-scaffold` (why the app folder is not pre-created).

## Result
_Pending the unticked verification items (they need GitHub and Docker)._

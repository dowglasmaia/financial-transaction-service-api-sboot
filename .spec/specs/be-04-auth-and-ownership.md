# SPEC-BE-04 — Auth0 authentication, CurrentUser, ownership

**Status:** Implemented + verified locally (98 tests green); manual Auth0 sign-in and PR/CI pending (owner) · **Depends on:** BE-03 · **Parallel with:** FE-02
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Replace the fake global identity with Auth0 JWT validation and make every read/write owner-scoped.

## Scope
- Implement `auth-auth0.md` backend section: resource server, issuer (trailing slash) + audience validation, role claim `https://financial-transaction/roles`→`ROLE_*` (D-30, no scopes), CORS, 401/403 in `{code,message}`, actuator hardening, mock JWKS issuer for local/CI
- Flyway `V3`: `usuario.external_subject` unique; `CurrentUser` in `services.client` (unknown `sub` → 403); remove `DBService` from the main path
- Ownership: statement and sums by caller's account; transaction by id scoped (foreign → 404); withdraw debits only own account and ledger follows debited account; deposit stays third-party

**Out of scope:** Auth0 dashboard setup (human); Frontend

## Tasks
- [x] Run skill `auth-oidc-config`: `check-inputs` -> `generate` -> follow `portal-checklist.md` -> `verify-tenant` (record the output)
  > ✅ 2026-11-02 — `check-inputs` → `VALID: 0 errors, 1 warning(s)` (W060: no test users yet); `verify-tenant` → 5x OK (issuer `https://dev-twljocmfwpvlxx1q.eu.auth0.com/`, jwks_uri, PKCE S256, 2 keys, RSA key). Verification: both scripts run (taken from branch `feat/auth-oidc-config-skill`); the portal checklist itself is for the owner (see the HUMAN task).
- [ ] HUMAN: Post-Login Action (roles claim) attached to the Login flow, two test users, sign-ups disabled; send both `sub` values (app + API already created; audience `https://api.financial-transaction.local`)
  > ⛔ 2026-11-02 — still open: the Action, the users and their `sub` values were not available in this run. Unblocks: the owner follows `portal/portal-checklist.md` (skill `auth-oidc-config`), then sets `SEED_OWNER_SUB` and does the manual sign-in. Everything else was proven with the mock issuer.
- [x] Dependencies + `SecurityFilterChain` + decoder/validators + converter
  > ✅ 2026-11-02 — `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-security-test` (test); `SecurityConfig`: lazy `SupplierJwtDecoder` from issuer discovery + timestamp (60 s skew), issuer, audience and subject validators; roles-claim → `ROLE_*` converter; stateless, CSRF off, CORS from properties. Red first: 11 of 18 security tests failed (e.g. wrong audience → 200, unlinked sub → 200); green after. Verification: `mvn test -Dtest=AuthenticationTest,CorsTest` → 18 run, 0 failures. Live probe: opt-in `LiveTenantDiscoveryTest` bootstraps the decoder from the real tenant → 1 run, 0 failures.
- [x] Method security per endpoint matrix
  > ✅ 2026-11-02 — DEVIATION (recorded): the matrix is one rule (role `customer` + linked subject), enforced in the filter chain (`/api/**` → `AuthorizationManager`; `/actuator/**` → ADMIN; health public), NOT with `@PreAuthorize`. Cause found by test: the generated `TransactionsApi` exposes handlers as default methods (`_getTransactions`) that call the controller overrides internally, so class- and method-level annotations were bypassed (missing role → 200 in two runs). Verification: `AuthenticationTest` 16 tests incl. missing role / `admin` only → 403.
- [x] Flyway `V3` + `CurrentUser`
  > ✅ 2026-11-02 — `V3__external_subject.sql` (unique `usuario.external_subject`), `CurrentUser` bean (`services.client`): `sub` → user → account; unlinked → 403 `{code:"FORBIDDEN"}`. `DBService` no longer provides an identity (only the local/test seed, which links `SEED_OWNER_SUB`). Verification: `FlywaySchemaTest.anIdentityProviderSubjectCanLinkToOnlyOneUser` + unlinked-sub tests → green.
- [x] Mock issuer; update tests to send tokens
  > ✅ 2026-11-02 — `support/MockIssuer`: JDK HTTP server serving discovery + JWKS, RS256 tokens (issuer with trailing slash), builders for wrong issuer/audience/expiry/forged key/no subject/no roles. All integration tests send the owner token. Verification: full `mvn verify` → 98 tests, 0 failures.
- [x] Two-user tests: cross-access → 404/403
  > ✅ 2026-11-02 — `OwnershipTest` (6): A reads B transaction id → 404; statements/totals only own rows; A debiting B account number → 404, nothing moves; own non-card withdrawal OK; B operates only on B; third-party deposit still allowed. Red first: 4 of 6 failed (A debited B → 204, foreign id → 200). F-04/F-05 characterizations flipped. Verification: `mvn verify` → 98 run, 0 failures.
- [x] Re-verify every `[VERIFY]` item in `auth-auth0.md`
  > ✅ 2026-11-02 — web search of current Auth0 docs/third-party pages: free plan 25,000 MAU (third-party sources; the older 7,500 figure is outdated; hard-limit behaviour not confirmed → re-check in the dashboard); default access-token lifetime for a custom API is 86400 s (24 h), max 2,592,000 s (**set a shorter value on the API**); Post-Login pattern `event.authorization.roles` + namespaced custom claim confirmed (roles must be assigned before login; the fallback to `customer` is ours); `@auth0/auth0-angular` quickstarts cover Angular up to 21 but the exact SDK version/zoneless support stays [VERIFY] for FE-03; issuer discovery against the real tenant confirmed (`LiveTenantDiscoveryTest`). Not verifiable without the dashboard: inactivity suspension, Universal Login branding limits, token-endpoint rate limits.

## Acceptance
- [x] No token/wrong iss/aud/expired → 401; token for the Auth0 Management API audience → 401; missing `customer` role → 403; unlinked `sub` → 403
  > ✅ 2026-11-02 — `AuthenticationTest` (16 tests, real signature/issuer/audience/expiry validation against the mock issuer). Verification: `mvn verify` → 0 failures.
- [x] User A cannot read or debit B's data (tests)
  > ✅ 2026-11-02 — `OwnershipTest`, see above.
- [x] Totals only over caller's rows
  > ✅ 2026-11-02 — `OwnershipTest.theStatementAndItsTotalsContainOnlyTheCallersRows` (sumIncoming/sumOutgoing/balance per customer).
- [x] No secrets/tokens in repo or logs
  > ✅ 2026-11-02 — `git grep -i secret|password` shows only the Maven wrapper and env placeholders; no client secret exists (SPA); the old log lines that printed the user id/account number in the account lookup were removed. Verification: git grep run; review of the changed classes.

## Minimal validation (required to close this spec)
- [x] No token → 401; wrong issuer/audience/expired → 401; missing `customer` role → 403; unlinked `sub` → 403 — EXECUTED (`AuthenticationTest`)
- [x] User A requests user B's transaction id → 404; user A cannot debit B's account — EXECUTED (`OwnershipTest`)
- [x] Statement totals contain only the caller's rows — EXECUTED (`OwnershipTest`)
- [ ] One manual sign-in against the real Auth0 tenant succeeds — **NOT EXECUTED**: needs the Action + a test user (human) and the owner typing the password, which is not an agent action. Procedure: set `SEED_OWNER_SUB` to the user `sub`, run the API (`SPRING_PROFILES_ACTIVE=local`), obtain an access token for audience `https://api.financial-transaction.local` (Authorization Code + PKCE, SPA `Transaction-app`), pipe only the token into the skill `decode-token.mjs`, then `GET /api/v1/transactions?initial-date=...&final-date=...` with it → 200.

## Stop and ask
- Auth0 setup (human)
- Spring Security dependencies
- Roles claim name (default `https://financial-transaction/roles`)
- Removing non-card withdraw entirely (contract change)

## Skills
auth-oidc-config, java-modern-engineering, java-persistence-performance

## Result
**Result (agent, 2026-11-02):** the API is an OAuth2 resource server (Auth0-ready) and every operation is owner-scoped. 98 tests, 0 failures, 64.6 % lines (643/996) via `mvn -B -ntp verify` in a Maven container. Commits: red security tests + mock issuer; JWT resource server/CurrentUser/V3; red ownership tests; ownership + flipped characterizations.

**Executed:** all tests above; issuer discovery + decoder bootstrap against the real tenant (opt-in `LiveTenantDiscoveryTest`); skill `check-inputs`/`verify-tenant`. **Not executed:** the manual Auth0 sign-in (human prerequisites missing); `docker compose up` with auth.

**Behaviour notes:** 401/403 bodies are `{code,message}` (`UNAUTHORIZED`/`FORBIDDEN`) with `WWW-Authenticate: Bearer`; foreign resources are 404; a foreign non-card withdrawal answers `404 "Account not found."`; third-party deposits stay allowed; roles claim `https://financial-transaction/roles`, no scopes (D-30); `/actuator/health` is public (details only for ADMIN), other actuator endpoints are ADMIN-only.

**Accepted limitations:** (1) `/actuator/prometheus` and metrics are closed to everyone until a management port/scrape credential exists (BE-08). (2) One account per user is assumed (`findFirstByUserId...`), not enforced by a constraint. (3) `AddAmountService.debit(number)` is now unused by main code (cleanup in BE-08). (4) Authorization lives in the filter chain, not in annotations (see the deviation above); BE-06 may revisit it when the controller interface is regenerated. (5) Issuer/audience defaults in `application.yaml` point to the owner dev tenant; override with `AUTH_ISSUER`/`AUTH_AUDIENCE`. (6) The Auth0 API access-token lifetime defaults to 24 h: shorten it in the dashboard (15-60 min).

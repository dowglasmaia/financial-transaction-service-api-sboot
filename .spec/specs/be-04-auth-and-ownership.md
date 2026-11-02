# SPEC-BE-04 — Auth0 authentication, CurrentUser, ownership

**Status:** Draft (not authorized) · **Depends on:** BE-03 · **Parallel with:** FE-02
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`. Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Replace the fake global identity with Auth0 JWT validation and make every read/write owner-scoped.

## Scope
- Implement `auth-auth0.md` backend section: resource server, issuer (trailing slash) + audience validation, role claim `https://financial-transaction/roles`→`ROLE_*` (D-30, no scopes), CORS, 401/403 in `{code,message}`, actuator hardening, mock JWKS issuer for local/CI
- Flyway `V3`: `usuario.external_subject` unique; `CurrentUser` in `services.client` (unknown `sub` → 403); remove `DBService` from the main path
- Ownership: statement and sums by caller's account; transaction by id scoped (foreign → 404); withdraw debits only own account and ledger follows debited account; deposit stays third-party

**Out of scope:** Auth0 dashboard setup (human); Frontend

## Tasks
- [ ] Run skill `auth-oidc-config`: `check-inputs` -> `generate` -> follow `portal-checklist.md` -> `verify-tenant` (record the output)
- [ ] HUMAN: Post-Login Action (roles claim) attached to the Login flow, two test users, sign-ups disabled; send both `sub` values (app + API already created; audience `https://api.financial-transaction.local`)
- [ ] Dependencies + `SecurityFilterChain` + decoder/validators + converter
- [ ] Method security per endpoint matrix
- [ ] Flyway `V3` + `CurrentUser`
- [ ] Mock issuer; update tests to send tokens
- [ ] Two-user tests: cross-access → 404/403
- [ ] Re-verify every `[VERIFY]` item in `auth-auth0.md`

## Acceptance
- [ ] No token/wrong iss/aud/expired → 401; token for the Auth0 Management API audience → 401; missing `customer` role → 403; unlinked `sub` → 403
- [ ] User A cannot read or debit B's data (tests)
- [ ] Totals only over caller's rows
- [ ] No secrets/tokens in repo or logs

## Minimal validation (required to close this spec)
- [ ] No token → 401; wrong issuer/audience/expired → 401; missing `customer` role → 403; unlinked `sub` → 403
- [ ] User A requests user B's transaction id → 404; user A cannot debit B's account
- [ ] Statement totals contain only the caller's rows
- [ ] One manual sign-in against the real Auth0 tenant succeeds

## Stop and ask
- Auth0 setup (human)
- Spring Security dependencies
- Roles claim name (default `https://financial-transaction/roles`)
- Removing non-card withdraw entirely (contract change)

## Skills
auth-oidc-config, java-modern-engineering, java-persistence-performance

## Result
_Filled on completion: result, checks run, accepted limitations._

# SPEC-SL-01 — Sign in and account (Auth0 login + `GET /accounts/me` + shell + Home header)

**Status:** Draft (not authorized) · **Depends on:** SL-00, BE-04 (merged; its manual sign-in is closed by this slice) · **Parallel with:** none
**Branch:** `slice/01-login-and-account` · **Track:** Backend + Frontend + E2E
**Absorbs:** FE-03 (auth part, Home header), BE-06 (`/accounts/me` only), BE-04's pending manual validation.
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md`, `../auth-auth0.md` (frontend section and pitfalls), `../spec-execution-flow.md` §1b, and the v2 reference (`Maia Bank v2.dc.html`: landing, sign-in, shell, Home header/balance). Protocol: `../shared/como-executar.md`.

## Objective
The first thing the owner can **open in the browser and use**: land on Maia Bank, sign in with Auth0 (Authorization Code + PKCE), arrive at a Home that shows *who* is signed in and *their* account and balance from the real API, and sign out. It proves the whole chain: Angular → Auth0 → Spring resource server → `CurrentUser` → PostgreSQL.

## Human action before
1. Auth0 → *Transaction-app* (Single Page Application): add `http://localhost:4200` to **Allowed Callback URLs**, **Allowed Logout URLs** and **Allowed Web Origins**; keep Cross-Origin Authentication off; grants Authorization Code + Refresh Token with rotation (see `../auth-auth0.md` §Auth0 configuration). Add `https://oauth.pstmn.io/v1/callback` for Postman.
2. The Post-Login Action that adds `https://financial-transaction/roles` is attached to the Login flow; both test users have role `customer`; sign-ups are disabled.
3. Run the API with `SPRING_PROFILES_ACTIVE=local`, `SEED_OWNER_SUB=<user 1 sub>` and (optional) `SEED_SECOND_SUB=<user 2 sub>`.
4. Infra up: `docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api` (API runs from the IDE, db on `localhost:5433`).

## Scope
**Contract (preview → approval → apply, `openapi-rest-contract`):** `GET /api/v1/accounts/me` → `200 {agency, number, balance}`; `401` (no/invalid token), `403` (valid token, no linked customer), documented with `bearerAuth`. Added to the existing OpenAPI file; generator migration waits for SL-02.

**Backend:** `AccountController`-style endpoint in the existing `controllers` package using `CurrentUser.account()`; no new package; ownership by construction (never takes an id from the caller).

**Frontend:**
- Libraries: verify `@auth0/auth0-angular` against Angular 21 **zoneless**; if incompatible, propose the alternative and stop (carried over from BE-04). Token storage decision: in-memory + refresh-token rotation by default (`../auth-auth0.md`); `localStorage` cache only as an explicit, recorded trade-off.
- `core/auth`: `AuthService` wrapper (login with the API `audience` always sent, callback handling, logout `returnTo`, `user` from the ID token, access token for the API), route **guard**, **interceptor** that adds `Authorization: Bearer` **only** to `API_BASE_URL` requests; 401 → one silent renew, else session-expired dialog → re-login; 403 → "No access to this app" page (unlinked user); writes are never auto-retried.
- `config.json`: `auth.{domain, clientId, audience}` (public values only; no secret exists for a SPA).
- OpenAPI types: a repeatable `npm run api:types` that generates TypeScript types from the contract (tool choice = Stop and ask: new devDependency).
- Screens (English, from v2): **Landing** (hero, feature strip, "Access my account"), **Sign-in** (redirecting state, error state with retry, "Secure sign-in with Auth0"), **Home header** (greeting from the ID token name, `Agency 0211 · Account 2022` chip, balance card with hide/show and count-up, quick-action tiles only for routes that exist), **account menu** (theme toggle, Sign out with confirm dialog), **session-expired dialog**, **403 page**, **404 page**. States: loading skeleton, error with request id and "Try again".
- Compose profile `api` so `docker compose --profile api up` also runs the API (the old commented block becomes a real profile); the IDE run configuration remains the default for development.

**E2E (introduced here, reused by every later slice):** Playwright + a **mock OIDC provider** in a compose profile `e2e` (so the agent can sign in without a password; the real Auth0 login stays a manual owner check). New devDependency (`@playwright/test`) and a pinned mock-IdP image are **Stop and ask**. Optional CI job `e2e` (workflow, path-filtered to both apps).

**Out of scope:** Home summary/chart/recent activity (SL-02), any operation (SL-03…06), registration/password flows (Auth0 Universal Login owns them), roles other than `customer`.

## Tasks
- [ ] HUMAN: Auth0 portal and local run (see *Human action before*)
- [ ] CONTRACT: preview `GET /accounts/me` and wait for approval
- [ ] BE tests first (red): own account for user 1 and user 2 differ; unlinked `sub` → 403; no token → 401; wrong audience → 401; response has exactly `{agency, number, balance}`; no id parameter exists
- [ ] BE implementation; `./mvnw -B verify` green
- [ ] FE: library check (zoneless) and token-storage decision (Stop and ask if it changes)
- [ ] FE tests first (red): guard redirects when unauthenticated; interceptor adds the header only for the API origin; 401 → one renew then dialog; logout clears state; no token in URL/console
- [ ] FE: `core/auth` + config + `api:types`
- [ ] FE: Landing, Sign-in, Home header, account menu, dialogs, 403/404 pages
- [ ] Compose profile `api`; README "run back and front together" section
- [ ] E2E infra (mock IdP profile + Playwright) and specs: unauthenticated `/home` → landing; sign in as user 1 → Home shows account 2022 balance from the API; user 2 → a different account/balance; sign out → landing; expired token → session-expired dialog; unlinked user → 403 page; axe on landing, sign-in, Home
- [ ] Postman: add `Account / me` with expected-status tests; refresh the "Auth and ownership checks" folder
- [ ] HUMAN: real Auth0 sign-in in the browser for both users; paste the observed results (status of `/accounts/me`, displayed account) — this also closes BE-04's manual validation

## Acceptance
- [ ] Real browser: sign in as user 1 → Home shows the seeded demo account and its balance from `/accounts/me`; as user 2 → their own (403 page if not seeded/linked)
- [ ] Reload keeps the session (per the chosen storage strategy); sign out returns to landing and the API rejects the old token on use after expiry
- [ ] Access token and ID token never appear in URLs, `console` or application logs; Bearer sent only to the API origin
- [ ] `GET /accounts/me` reveals no other customer's data under any input
- [ ] Both pipelines green (`api-ci`, `app-ci`) on the PR

## Minimal validation (required to close this spec)
- [ ] `./mvnw -B verify` green (state test count and coverage)
- [ ] `npm ci && npm run build && npm test` green; `npm run api:types` reproducible (no diff on rerun)
- [ ] E2E with the mock IdP green on 1280 / 768 / 390 (state which ran)
- [ ] One manual sign-in per test user against the real Auth0 tenant (owner)
- [ ] axe: no serious violations on landing, sign-in, Home

## Stop and ask
- OpenAPI preview approval; new devDependencies (`@playwright/test`, type generator); mock IdP image; OIDC library incompatible with Angular 21 zoneless; storing tokens outside memory; CI e2e job cost

## Skills
openapi-rest-contract, java-modern-engineering, angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max, auth-oidc-config (portal guide)

## Result
_Filled on completion: result, checks run, accepted limitations._

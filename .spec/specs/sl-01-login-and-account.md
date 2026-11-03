# SPEC-SL-01 — Sign in and account (Auth0 login + `GET /accounts/me` + shell + Home header)

**Status:** Implemented and verified with the mock IdP; **open: owner's manual sign-in on the real Auth0 tenant, the Auth0 portal URLs, and green `api-ci`/`app-ci` on the PR** (see *Open items*) · **Depends on:** SL-00, BE-04 (ARCH-02 is **not** required: owner decision 2026-11-03, D-44) (merged; its manual sign-in is closed by this slice) · **Parallel with:** none
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

**Backend (as built, current layered structure):** `controllers.AccountController implements AccountsApi` resolves the account through the existing `CurrentUser`; ownership by construction (it takes no id). **ARCH-01/02 later move it into the `account` feature** (`account.application.AccountService` + `account.infra.web`); until then it follows the code that exists today (owner decision D-44).

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
  > ⏳ 2026-11-03 19:30 +0000 — Not done by the agent (needs your dashboard): add `http://localhost:4200` to Allowed Callback URLs, Logout URLs and Web Origins of *Transaction-app* (and `https://oauth.pstmn.io/v1/callback` for Postman). The local stack was run for the e2e (mock IdP); the real tenant was not touched.
- [x] CONTRACT: preview `GET /accounts/me` and wait for approval
  > ✅ 2026-11-03 19:30 +0000 — Preview shown and **approved by the owner** (question 2026-11-03). Applied to `swagger/financial-transaction-openapi.yaml` (+53 lines: path, `AccountResponse`, `bearerAuth`); `mvn generate-resources` generated `AccountsApi` and `AccountResponseDTO`. Found while generating: a plain YAML scalar with `: ` broke the parser, reworded. No existing operation changed.
- [x] BE tests first (red): own account for user 1 and user 2 differ; unlinked `sub` → 403; no token → 401; wrong audience → 401; response has exactly `{agency, number, balance}`; no id parameter exists
  > ✅ 2026-11-03 19:30 +0000 — `AccountMeTest` (8 cases) written first. Red: `-Dtest=AccountMeTest` → 3 failures (the 200 cases returned 404); the 401/403 cases already passed through the existing filter chain. Cases: exact fields, user 2 differs, parameters cannot select another account, no token, wrong audience, Management-API audience, unlinked sub, missing `customer` role.
- [x] BE implementation; `./mvnw -B verify` green
  > ✅ 2026-11-03 19:30 +0000 — `controllers.AccountController` (no id parameter). EXECUTED `mvn clean verify` (container, JDK 17): **116 tests, 0 failures, 1 skipped** (opt-in live-tenant probe), BUILD SUCCESS, line coverage 62.9% (675/1073). Also: seed now relinks an existing second customer to the configured subject (test first, red then green) so switching between the real tenant and the mock never leaves a customer unlinked.
- [x] FE: library check (zoneless) and token-storage decision (Stop and ask if it changes)
  > ✅ 2026-11-03 19:30 +0000 — `@auth0/auth0-angular` 2.15.0 (peer `@angular/core >= 13`) works with Angular 21 **zoneless**: build, 174 unit tests and 21 browser tests pass. Storage: **tokens in memory** (`cacheLocation: 'memory'`, rotating refresh token), never in web storage. Trade-off to decide: a page **reload** loses the in-memory tokens, so the session survives only if the identity provider's own session allows silent sign-in (third-party cookies); the mock IdP has no session, so a reload signs out there. Keeping the session across reloads would mean `localStorage` (XSS exposure). **Decided by the owner (2026-11-03): tokens stay in memory** (market standard and the safest of the two; a BFF would be safer still but needs a server). Accepted trade-off: a reload may need one click on "Access my account" (no password again while the Auth0 session lives). If silent sign-in proves unreliable the next step is an Auth0 custom domain (first-party cookies), not `localStorage`.
- [x] FE tests first (red): guard redirects when unauthenticated; interceptor adds the header only for the API origin; 401 → one renew then dialog; logout clears state; no token in URL/console
  > ✅ 2026-11-03 19:30 +0000 — Written first (20 specs: config, guard, session wrapper, interceptor). Red: modules did not exist (`Could not resolve …`). Covered: header only for `/api/v1/…` (not `/apiary`, not other hosts), one silent renewal + one retry on a GET 401, a POST 401 is never retried, no token → request not sent + session expired, 403/422/500 untouched, logout returns to the origin and clears the expired flag, nothing written to console or web storage.
- [x] FE: `core/auth` + config + `api:types`
  > ✅ 2026-11-03 19:30 +0000 — `core/auth` (`SessionService` abstraction, `Auth0SessionService`, `buildAuthConfig`, `authGuard`), interceptor rewritten, runtime config now `{domain, clientId, audience}` (public values; `apiBaseUrl` `/api/v1`). `npm run api:types` (openapi-typescript 7.13) generates `core/api/schema.d.ts`; EXECUTED twice, **no diff on the second run**.
- [x] FE: Landing, Sign-in, Home header, account menu, dialogs, 403/404 pages
  > ✅ 2026-11-03 19:30 +0000 — Landing (hero, 3 features, "Signing you in…" during the callback), Sign-in (redirecting/error states, "Secure sign-in with Auth0"), Home (greeting, `Agency 0211 · Account 2022`, balance with hide/show and count-up, skeleton, error + request id + retry, 403 → "No access"), account menu, sign-out confirmation and session-expired dialogs, 404 page, public vs signed-in frame. Footer shows the author's photo (192 px avatar in `public/img`, text alternative, initials fallback). 174 unit tests. Viewed in a real browser at 1280 and 390 (landing, Home, footer). Quick actions: none rendered, no operation route exists yet. Fix found by eye: the landing tiles used the `ph-fill` weight (not shipped) → `ph-bold`, with a regression test.
- [x] Compose profile `api`; README "run back and front together" section
  > ✅ 2026-11-03 19:30 +0000 — `docker-compose.yml`: API behind `--profile api`; Postgres host port `${POSTGRES_PORT:-5433}`; Kafdrop pointed at the in-network listener. API default port 8088 (matches the proxy, Postman and Docker). The image build now skips tests (`mvn package -DskipTests`: the suite needs Docker and runs in CI/IDE; the old `mvn install` failed inside the build). Root README section added. EXECUTED: `docker compose … up -d --build` → API health `UP`.
- [x] E2E infra (mock IdP profile + Playwright) and specs: unauthenticated `/home` → landing; sign in as user 1 → Home shows account 2022 balance from the API; user 2 → a different account/balance; sign out → landing; expired token → session-expired dialog; unlinked user → 403 page; axe on landing, sign-in, Home
  > ✅ 2026-11-03 19:30 +0000 — Mock IdP in Node (no dependency, `e2e/mock-idp/server.mjs`, Auth0 path layout, PKCE, refresh rotation) + `docker-compose.e2e.yml` (API trusts the mock issuer; two mock customers seeded) + Playwright config (3 viewports). EXECUTED `npm run e2e`: **21 passed (7 scenarios × 1280/768/390)** including axe (wcag2a/aa/21a/21aa, serious+critical) on landing, sign-in and Home, a token-leak check (URLs, console, web storage), user 1 vs user 2, no-access, sign-out (the session is really gone), expired token → dialog. Not added: the optional `e2e` CI job (cost decision).
- [ ] Postman: add `Account / me` with expected-status tests; refresh the "Auth and ownership checks" folder
  > ⏳ 2026-11-03 19:30 +0000 — Parcial. Written: two requests (200 with exactly 3 fields and the caller's account; 401 without a token) in the checks folder. Not executed in Postman (needs a real interactive login); the same cases run in `AccountMeTest` and the e2e. Próxima ação: you run the folder after the manual sign-in.
- [ ] HUMAN: real Auth0 sign-in in the browser for both users; paste the observed results (status of `/accounts/me`, displayed account) — this also closes BE-04's manual validation
  > ⛔ 2026-11-03 19:30 +0000 — Not executed: only the owner can type the test users' passwords. Para desbloquear: do the portal step above, run the API with `SEED_OWNER_SUB`/`SEED_SECOND_SUB`, open http://localhost:4200, sign in as each user and report the account shown (or the "No access" page).

## Acceptance
- [ ] Real browser: sign in as user 1 → Home shows the seeded demo account and its balance from `/accounts/me`; as user 2 → their own (403 page if not seeded/linked)
  > ⛔ 2026-11-03 19:30 +0000 — Real Auth0: pending the owner (see the HUMAN task). With the **mock IdP** this is proven: user 1 → account 2022 / R$ 5,000.00, user 2 → account 3033 / R$ 1,000.00, unlinked → "No access" (21 browser tests).
- [ ] Reload keeps the session (per the chosen storage strategy); sign out returns to landing and the API rejects the old token on use after expiry
  > ⏳ 2026-11-03 19:30 +0000 — Sign out returns to the landing page and the session is gone (e2e). An expired token gets 401 → one renewal → the dialog (e2e). **Reload does not keep the session with the in-memory strategy** unless the identity provider session allows silent sign-in. **Decided (owner, 2026-11-03): keep tokens in memory**; the owner checks the reload behaviour once during the real sign-in.
- [ ] Access token and ID token never appear in URLs, `console` or application logs; Bearer sent only to the API origin
  > ⏳ 2026-11-03 19:30 +0000 — Executed: no JWT-shaped string or `access_token`/`id_token`/`refresh_token` parameter in any visited URL, in the console, or in local/session storage (e2e); Bearer only for `/api/v1/…` (unit tests). Not executed: a review of the API's own log output for tokens.
- [x] `GET /accounts/me` reveals no other customer's data under any input
  > ✅ 2026-11-03 19:30 +0000 — `AccountMeTest`: query parameters (`number`, `accountId` of another customer) are ignored; each token only ever returns its own account; unlinked → 403, no token/foreign audience → 401.
- [ ] Both pipelines green (`api-ci`, `app-ci`) on the PR
  > ⛔ 2026-11-03 19:30 +0000 — Not executed: needs the PR (the owner opens it). Para desbloquear: green `api-ci` and `app-ci`. Note: the initial bundle is now 725.60 kB (the Auth0 SDK); the warning budget was raised from 500 kB to 800 kB (error limit stays 1 MB).

## Minimal validation (required to close this spec)
- [x] `./mvnw -B verify` green (state test count and coverage)
  > ✅ 2026-11-03 19:30 +0000 — EXECUTED (see the backend task): 116 tests, 0 failures, 62.9% lines.
- [x] `npm ci && npm run build && npm test` green; `npm run api:types` reproducible (no diff on rerun)
  > ✅ 2026-11-03 19:30 +0000 — EXECUTED: `npm run build` OK (initial 725.60 kB), `npm test` → 24 files, **174 tests passed**; `api:types` rerun with no diff. Re-verified from scratch after the last change: `rm -rf node_modules dist .angular && npm ci && npm run build && npm test` → build OK (initial 725.62 kB, no budget warning), 174 passed.
- [x] E2E with the mock IdP green on 1280 / 768 / 390 (state which ran)
  > ✅ 2026-11-03 19:30 +0000 — EXECUTED: 21 passed on `desktop-1280`, `tablet-768`, `mobile-390`.
- [ ] One manual sign-in per test user against the real Auth0 tenant (owner)
  > ⛔ 2026-11-03 19:30 +0000 — Pending the owner (see the HUMAN task).
- [x] axe: no serious violations on landing, sign-in, Home
  > ✅ 2026-11-03 19:30 +0000 — EXECUTED inside the e2e on the 3 viewports (landing, sign-in, Home): 0 serious/critical.

## Stop and ask
- OpenAPI preview approval; new devDependencies (`@playwright/test`, type generator); mock IdP image; OIDC library incompatible with Angular 21 zoneless; storing tokens outside memory; CI e2e job cost

## Skills
openapi-rest-contract, java-modern-engineering, angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max, auth-oidc-config (portal guide)

## Result
The whole sign-in chain works end to end: **browser → identity provider → Spring resource server → PostgreSQL**. A visitor lands on Maia Bank, signs in (Authorization Code + PKCE through `@auth0/auth0-angular`, tokens in memory), arrives at a Home that shows their name, `Agency · Account` and the balance from `GET /accounts/me`, and can sign out (with confirmation); an unlinked identity gets a "No access" page and an expired session a dialog. The footer shows the author's photo.

**Executed:** backend `verify` (116 tests), `GET /accounts/me` red → green, contract generated, 174 unit tests, `api:types` reproducible, production build, **21 browser tests** (3 viewports, axe, token-leak check, two customers) against the real API in Docker and a mock IdP, plus a visual check of landing/Home/footer.

**Not executed / open:** see *Open items*. **Accepted limitations:** the backend was built in the current layered structure (ARCH-01/02 will move `AccountController`); a reload signs the user out with in-memory tokens; Postman requests are written but not run; the optional `e2e` CI job was not added.

## Open items (how to resume)
1. **Auth0 portal** (you): add `http://localhost:4200` to Callback/Logout/Web-Origin URLs of *Transaction-app*.
2. **Manual sign-in** (you): real Auth0 as user 1 and user 2; report the account shown; run the Postman checks folder.
3. ~~Decision~~ **Decided (2026-11-03): tokens stay in memory.** During the real sign-in, reload once and note whether silent sign-in keeps the session (if not: one click to sign in again; the fix would be an Auth0 custom domain, not `localStorage`).
4. **CI** (PR): `api-ci` and `app-ci` green.
5. **Optional**: add the `e2e` job to CI (needs Docker + the compose overlay; cost decision).
After these, close the spec (tick, set *Done* in `README.md`) and start **ARCH-01** (the backend refactor you wanted next).

# Authentication & Authorization Specification — Okta (OAuth 2.0 / OIDC)

## Objective and scope
Specify how authentication and authorization will work for the Spring Boot API and the new Angular app, using **Okta (Integrator Free Plan)** as Identity Provider with **OAuth 2.0 + OpenID Connect**, **Authorization Code Flow with PKCE**, **JWT validation**, **roles and permissions**. Resolves Q-03 (decision D-08) and closes finding **F-01** and the access-control parts of **F-04/F-05** once implemented.
**Specification only. Nothing is implemented, no Okta org was created or contacted, no credential exists.** Vendor facts come from a web search on 2026-11-02 (Okta docs + community threads); every Okta-specific setting must be re-verified in the Okta Admin Console/docs at implementation time (tag **[VERIFY]**).

## Decisions
| ID | Decision | Notes |
|---|---|---|
| D-08 | IdP = Okta Integrator Free Plan; protocol OIDC/OAuth 2.0 | Decided |
| D-09 | Backend is a pure **OAuth2 Resource Server** (Spring Security `oauth2ResourceServer().jwt()`); no sessions, no login pages, no client secret | Standard Spring Security; the Okta Spring Boot starter is **not** required (fewer vendor-specific dependencies) |
| D-10 | Frontend is a **public SPA client**: Authorization Code + **PKCE**, no client secret, no implicit flow | Mandatory for browser apps |
| D-11 | Authorization = **scopes (permissions) + groups (roles)** from a **custom authorization server** | Org authorization server tokens are for Okta APIs, not for custom APIs **[VERIFY]** |
| D-12 | Token-to-account link stored in our DB (`usuario.external_subject`) | Okta is identity only; money data never goes to Okta |
| D-13 | Accept the free-plan constraints for development/portfolio use; revisit before any real production use | See §Limitations |

## Architecture
```
 Angular SPA ──(1) /authorize + PKCE──► Okta (custom authorization server "financial-transaction")
      ▲  │◄─(2) code ─────────────────┘      │ JWKS (public keys)
      │  └─(3) code+verifier → /token ───────►│
      │       access_token (JWT), id_token, [refresh_token]
      │
      └──(4) HTTPS  Authorization: Bearer <access_token> ──► Spring Boot API (resource server)
                                                           validates signature (cached JWKS), iss, aud, exp, scopes
                                                           → CurrentUser(sub) → local user → account → use case
```
The API never calls Okta per request: it validates JWTs locally against cached JWKS (important because the free plan has lower rate limits).

## Okta configuration (to be performed by a human in the Okta org)
1. **Org:** create an Integrator Free Plan org (record org URL in the team's secret store, not in the repo).
2. **Authorization server:** use a **custom** one (`default` is acceptable for dev; prefer a dedicated `financial-transaction`): issuer `https://{org}.okta.com/oauth2/{serverId}`, **audience** `api://financial-transaction`.
3. **Scopes (permissions)** on that server: `transactions:read`, `transactions:write` (finer split optional: `pix:send`, `deposit:create`, `withdrawal:create`, `recharge:create`). Keep them few; authorization checks map 1:1 to endpoints (matrix below).
4. **Groups (roles):** `customer` (default), `admin` (reserved, no endpoint today). **No `operator` role (D-21).** Assign users to groups.
5. **Claims:** add a `groups` claim to the **access token** (filter e.g. `Matches regex ^(customer|admin)$`) — on custom servers the `groups` scope/claim is **not preconfigured** and must be created manually **[VERIFY]**. Optional custom claim `account_ref` is *not* recommended (keep account data out of the IdP).
6. **Access policy / rules:** policy for the SPA app granting the scopes above; Authorization Code (+ Refresh Token) grant allowed; access-token lifetime 15–60 min; refresh-token rotation on (SPA); ID-token lifetime default.
7. **Applications:**
   * `financial-transaction-web` — *OIDC, Single-Page App*, PKCE required, **no client secret**; sign-in redirect URIs (`http://localhost:4200/login/callback`, prod URL), sign-out redirect URIs, **Trusted Origins** (CORS + redirect) for each SPA origin; grant types: Authorization Code, Refresh Token; assignments: group `customer`.
   * The API needs **no Okta application**: it only trusts the issuer/audience.
8. **Users:** the plan allows **10 active users** — plan test users accordingly (see Limitations). Self-registration/MFA policies: configure only what the plan allows **[VERIFY]**.

## Backend specification (Spring Boot / Spring Security)
**Dependencies:** `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`; tests `spring-security-test`.
**Configuration (externalized, no secrets):**
```yaml
spring.security.oauth2.resourceserver.jwt.issuer-uri: ${OKTA_ISSUER}      # https://{org}.okta.com/oauth2/{serverId}
app.security.audience: ${OKTA_AUDIENCE}                                     # api://financial-transaction
app.security.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS}                  # SPA origin(s)
```
**Behaviour:**
* `SecurityFilterChain`: stateless, CSRF disabled (bearer-only API, no cookies), CORS from properties (currently [ABSENT]), `/actuator/health/**` public, other actuator endpoints restricted (own management port / role `admin`), `anyRequest().authenticated()`.
* `JwtDecoder`: built from `issuer-uri` **plus validators** `JwtTimestampValidator` and an **audience validator** (the default decoder does not check `aud` → add explicitly); clock skew ≤ 60 s; reject tokens missing `sub`.
* `JwtAuthenticationConverter`: authorities = `SCOPE_*` from `scp` (Okta puts scopes in the `scp` array claim, Spring defaults read `scope`/`scp` — confirm **[VERIFY]**) **+** `ROLE_<GROUP>` from the `groups` claim (custom converter; the Okta starter's `groupsClaim` property is an alternative but is not used by D-09).
* Method security (`@EnableMethodSecurity`): `@PreAuthorize("hasAuthority('SCOPE_transactions:write')")` etc. at the use-case/controller boundary.
* **`CurrentUser` port**: resolves `sub` → `usuario` via new column `external_subject` (unique) → account(s). Replaces `DBService.extractUserAccountFromJwt` (the name already anticipated JWT). Unknown `sub` → 403 `{code:"FORBIDDEN"}` (no auto-provisioning; user↔account linking is a manual/seed operation — decided D-21).
* **Ownership enforcement (authoritative):** scopes say *what kind* of action; ownership says *whose data*. Every repository access is constrained by the authenticated user's account(s) (BR-51, BR-54 fixes). Foreign resource → 404.
* Error mapping: 401 (`WWW-Authenticate: Bearer`, body `{code:"UNAUTHORIZED",message}`), 403 (`{code:"FORBIDDEN",message}`) keeping the existing `{code,message}` shape.
* Logging: never log tokens; log `sub` hash/user id only; add `traceId`.
* Dev profile: a **local mock issuer** (mock OAuth2 server / WireMock JWKS) so contributors and CI do not depend on Okta or consume its 10 users.

**Authorization matrix**
| Endpoint | Authority required | Ownership rule |
|---|---|---|
| `GET /transactions`, `GET /transactions/{id}` | `SCOPE_transactions:read` | Only the caller's account(s) |
| `POST /transactions/send-pix` | `SCOPE_transactions:write` | Debits caller's account |
| `POST /transactions/recharge-mobile` | `SCOPE_transactions:write` | Debits caller's account |
| `POST /transactions/atm-withdrawal` | `SCOPE_transactions:write` | Decided: debited account must belong to caller; ledger follows debited account |
| `POST /transactions/deposit` | `SCOPE_transactions:write` | Third-party deposits allowed (target any existing account) |
| `GET /accounts/me` (proposed AP-8) | `SCOPE_transactions:read` | Caller's account |
| Actuator (non-health) | role `admin` or network-restricted port | — |
Role usage: `customer` is required for all the above (checked via `ROLE_CUSTOMER`); `admin` has no endpoints today and must not be invented.

## Frontend specification (Angular 21)
* **Library decision D-FE-4:** either `@okta/okta-auth-js` (+ `@okta/okta-angular`) or a standards-based library (`angular-auth-oidc-client` / `angular-oauth2-oidc`). Recommendation: standards-based OIDC library to avoid vendor lock-in; use Okta's SDK only if it explicitly supports Angular 21 and zoneless **[VERIFY compatibility on the day]**. Whichever is chosen, wrap it behind an `AuthService` interface in `core/auth` so features never import the SDK.
* **Flow:** `authGuard` (functional `CanActivateFn`) → if no valid session, store `returnUrl` and redirect to `/authorize` (code + PKCE, `scope=openid profile email offline_access transactions:read transactions:write`, state/nonce handled by the lib) → `/login/callback` exchanges code → tokens → navigate to `returnUrl`.
* **Token storage:** prefer **in-memory** access token; refresh token only with rotation; note that SDK defaults often use `sessionStorage`/`localStorage` (XSS exposure) — configure deliberately and record the trade-off. A BFF is the stricter alternative (out of scope; decision D-FE-5 if security review requires it).
* **Interceptor:** `authInterceptor` adds `Authorization: Bearer` only for `API_BASE_URL` requests; on 401 → one silent renew attempt, else re-login; 403 → "no permission" page. No tokens in URLs, logs or error reports.
* **Authorization in UI:** `hasScope()/hasRole()` signals only to hide/disable navigation; the server is authoritative (`angular-architect`: guards are UX, not security).
* **Logout:** RP-initiated logout (`/logout` with `id_token_hint` + `post_logout_redirect_uri`) and local state wipe.
* **Runtime config** (`config.json`): `issuer`, `clientId`, `redirectUri`, `postLogoutRedirectUri`, `scopes`, `apiBaseUrl`. `clientId` is public; **no secret exists**.
* **Dev-open mode** (no login, "Demo" banner) remains only for builds with `auth.enabled=false` against the local mock issuer.

## End-to-end sequences
1. **Login:** SPA → Okta authorize (PKCE) → user authenticates at Okta → redirect with code → SPA token request → tokens held per policy.
2. **API call:** SPA → API with bearer → API validates `iss`, `aud`, `exp/nbf`, signature (JWKS cached, refreshed on unknown `kid`) → authorities → controller → `CurrentUser` → use case with ownership.
3. **Expiry:** API 401 → SPA silent renew (refresh token or hidden iframe/`prompt=none` per lib) → retry GET once; writes are **not** auto-retried (F-21).
4. **Logout:** SPA → Okta logout → post-logout redirect.

## Free plan limitations & risks (Okta docs as of 2026-11-02, **[VERIFY]**)
* **10 active users** (docs org-defaults page) — an older community post mentions 100 MAU for the legacy Developer Edition; use 10 for planning.
* Org **deactivates after 180 days without sign-ins** (unless an OIN submission); not expiring otherwise.
* **No Okta Support** access.
* Lower **API rate limits** than paid tiers (e.g. 100 req/min on session and authentication endpoints) → keep JWT validation local, avoid calling Okta `userinfo` per request, load-test login bursts.
* Email templates not editable; email automation unavailable → custom verification/reset emails limited.
* Features included per docs: SSO, API Access Management (needed for the custom authorization server), Workflows (limited). MFA/policy depth, branding and custom domain availability **[VERIFY]**.
* Intended for integration development; **confirm licensing/terms before using it for real customers**. A migration path exists because the app uses standard OIDC: replacing the issuer/audience/clientId (and claim names) moves to Keycloak/Auth0/Cognito.
* Single IdP org = single point of failure; no data residency choice evaluated.
* Known pitfalls from community threads: `groups` claim missing when issuer ≠ the server where the claim was defined; 403 from the resource server when issuer/audience mismatch; groups need custom mapping to Spring authorities.

## Security requirements
No client secret in SPA; PKCE S256; exact redirect URI matching; Trusted Origins limited; access token lifetime short; audience validated; TLS everywhere outside localhost; CORS limited to configured origins; rate limiting at gateway; secrets (none needed for SPA/API, only org URL/ids) in env; dependency scanning; token redaction in logs.

## Tests (acceptance)
Backend (Spring Security Test `jwt()` and a mock JWKS server): no token → 401; expired/wrong `iss`/wrong `aud` → 401; missing scope → 403; `customer` without ownership → 404; `sub` unlinked → 403; scope + role mapping unit-tested; CORS preflight allowed only for configured origin; actuator protection verified. Frontend: guard redirects when unauthenticated; interceptor attaches header only to API origin; 401 → renew once; logout clears state; e2e with a dedicated Okta test user **in a non-CI environment** (consumes 1 of 10 users).
Done when: F-01 closed, `BR-04` replaced, all above pass, `git grep` shows no tokens/secrets, runbook for Okta setup added to `.spec/`.

## Implementation tasks (not started; follow `shared/como-executar.md`)
- [ ] Human: create Okta org, custom authorization server, scopes, groups claim, SPA app, trusted origins, test users (document ids, not secrets).
- [ ] Backend: dependencies, `SecurityFilterChain`, decoder + audience validator, authority converter, method security, `CurrentUser` adapter, Flyway `external_subject`, error handlers, CORS, actuator hardening, mock issuer for dev/CI, tests.
- [ ] Contract: add `bearerAuth` scheme, 401/403 responses, scopes per operation (through `openapi-rest-contract` approval flow).
- [ ] Frontend: `core/auth` wrapper, guard, interceptor, callback route, logout, config loader, e2e.
- [ ] Re-verify every **[VERIFY]** item against current Okta docs.

## Skills used
`springboot-patterns` (consulted; it has no security section, so Spring Security design follows standard resource-server practice, not a skill recipe), `java-modern-engineering` (security as correctness requirement), `java-persistence-performance` (ownership scoping on every query, Flyway for the new column), `openapi-rest-contract` (security scheme conventions), `angular-architect` (guards are UX not security; token/CSRF decisions need approved architecture), `angular-frontend-scaffold`/`angular-module-scaffold` (where auth plugs into the scaffold), `ui-ux-pro-max` (session-expiry and error UX). No Okta-specific skill is installed.

Sources: [Okta org defaults](https://developer.okta.com/docs/reference/org-defaults/), [Okta rate limits](https://developer.okta.com/docs/reference/rl-additional-limits), [Okta global rate limits](https://developer.okta.com/docs/reference/rl-global-enduser), [Integrator Free Plan summary](https://freetier.co/directory/products/okta-integrator-free-plan) (third-party), Okta developer-forum threads on [groups claim](https://devforum.okta.com/t/how-to-add-groups-claim-for-the-org-authorization-server-for-a-specific-application/12009), [roles in resource server](https://devforum.okta.com/t/accessing-roles-and-authorities-inside-resource-server/16401) and [403 with /oauth2/default](https://devforum.okta.com/t/implicite-flow-okta-403-error-api-oauth2resourceserver-jwt/10486).

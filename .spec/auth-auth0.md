# Authentication & Authorization Specification — Auth0 (OAuth 2.0 / OIDC)

## Objective and scope
Specify how authentication and authorization work for the Spring Boot API and the Angular app, using **Auth0 (free plan)** as Identity Provider with **OAuth 2.0 + OpenID Connect**, **Authorization Code Flow with PKCE**, **JWT validation** and a **role claim**. Closes finding **F-01** and the access-control parts of **F-04/F-05** once implemented (BE-04).
**Supersedes the Okta specification (D-08 changed by D-29).** The architecture is provider-neutral: the API only trusts an issuer, an audience and a role claim, so switching IdP again is a configuration change.
Vendor facts must be re-verified in the Auth0 dashboard/docs at implementation time (tag **[VERIFY]**).

## Decisions
| ID | Decision | Notes |
|---|---|---|
| D-08 | ~~Okta~~ → superseded by **D-29** | Okta was never configured |
| D-09 | Backend is a pure **OAuth2 Resource Server** (Spring Security `oauth2ResourceServer().jwt()`); no sessions, no login pages, no client secret | No vendor starter/SDK in the API |
| D-10 | Frontend is a **public SPA client**: Authorization Code + **PKCE**, no client secret, no implicit flow | The Auth0 application type MUST be *Single Page Application* |
| D-11 | ~~Scopes + groups~~ → superseded by **D-30** | |
| D-12 | Token-to-account link stored in our DB (`usuario.external_subject` = the token `sub`, e.g. `auth0\|65f…`) | The IdP is identity only; money data never goes to it |
| D-13 | Accept free-plan limits for development/portfolio use; revisit before real production use | See §Limitations |
| **D-29** | **IdP = Auth0** (tenant `dev-twljocmfwpvlxx1q.eu.auth0.com`), protocol OIDC/OAuth 2.0 | Replaces Okta; reason: owner already has the tenant |
| **D-30** | Authorization = **role claim** `https://financial-transaction/roles` (array) added to the **access token** by a Post-Login Action; only role `customer` exists (D-21). **No scopes/permissions are defined** | One role, every endpoint requires `ROLE_CUSTOMER`; ownership (not scopes) decides whose data. Fine-grained scopes can be added later via Auth0 RBAC without changing the architecture |

## Non-secret configuration facts (safe to commit)
| Item | Value |
|---|---|
| Tenant domain | `dev-twljocmfwpvlxx1q.eu.auth0.com` |
| Issuer (**trailing slash is part of it**) | `https://dev-twljocmfwpvlxx1q.eu.auth0.com/` |
| JWKS | `https://dev-twljocmfwpvlxx1q.eu.auth0.com/.well-known/jwks.json` |
| API name / audience (Identifier) | `Financial Transaction API` / `https://api.financial-transaction.local` (RS256) |
| SPA application | `Transaction-app`, type *Single Page Application*, Client ID `Sy50My7UVdXFjIX7VCwm35LLyzNosKUr` (public) |
| Roles claim | `https://financial-transaction/roles` |
**Never commit or paste:** the Client Secret (the SPA has none to use), user passwords, access/refresh tokens, Management API tokens. The built-in *Auth0 Management API* (`…/api/v2/`) is NOT our API and its audience must never be accepted.

## Architecture
```
 Angular SPA ──(1) /authorize + PKCE + audience──► Auth0 (Universal Login)
      ▲  │◄─(2) code ──────────────────────────────┘      │ JWKS (public keys)
      │  └─(3) code+verifier → /oauth/token ─────────────►│
      │       access_token (JWT, aud = our API), id_token, [refresh_token]
      │
      └──(4) HTTPS  Authorization: Bearer <access_token> ──► Spring Boot API (resource server)
                                                          validates signature (cached JWKS), iss, aud, exp, role claim
                                                          → CurrentUser(sub) → local user → account → use case
```
The API never calls Auth0 per request: it validates JWTs locally against the cached JWKS. Without the `audience` parameter on `/authorize`, Auth0 returns an opaque token that the API cannot validate — the SPA must always send it.

## Auth0 configuration (human, in the dashboard)
**Use the project skill `auth-oidc-config` (`.claude/skills/auth-oidc-config/`)**: validate the inputs, generate the backend/frontend config, the Action and a checklist with your values, then verify the tenant and a real token. Inputs for this project: `.claude/skills/auth-oidc-config/examples/financial-transaction.inputs.json`. The summary below is the same content in prose; the skill's generated files win if they differ.
1. **Application** `Transaction-app`: type *Single Page Application*; Allowed Callback URLs `http://localhost:4200` (+ deployed URL); Allowed Logout URLs and Allowed Web Origins the same; *Cross-Origin Authentication* off; grant types Authorization Code + Refresh Token; **Refresh Token Rotation** on; Application Login URI optional.
2. **API** `Financial Transaction API`: Identifier `https://api.financial-transaction.local` (immutable), RS256, *Allow Offline Access* on; Application Access: user-delegated = per-app authorization with `Transaction-app`, client access = no apps; no permissions defined (D-30). Access-token lifetime 15–60 min **[VERIFY default]**.
3. **Post-Login Action** (deploy and attach to the *Login* flow). `event.authorization.roles` is empty when the user has no Auth0 role, so default to `customer` (the only role, D-21):
   ```js
   exports.onExecutePostLogin = async (event, api) => {
     const ns = 'https://financial-transaction/roles';
     api.accessToken.setCustomClaim(ns, event.authorization?.roles?.length ? event.authorization.roles : ['customer']);
   };
   ```
   Custom claims in access tokens must be **namespaced** (a URL), which is why the claim name is a URL.
4. **Users:** two test users in the Database connection (`Username-Password-Authentication`); **Disable Sign Ups** on that connection. Record each `user_id` (the token `sub`) for the seed/linking step; do not record passwords here.

## Backend specification (Spring Boot / Spring Security)
**Dependencies:** `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`; tests `spring-security-test`. No Auth0 SDK.
**Configuration (externalized, no secrets):**
```yaml
spring.security.oauth2.resourceserver.jwt.issuer-uri: ${AUTH_ISSUER}       # https://dev-twljocmfwpvlxx1q.eu.auth0.com/  (trailing slash)
app.security.audience: ${AUTH_AUDIENCE}                                     # https://api.financial-transaction.local
app.security.roles-claim: ${AUTH_ROLES_CLAIM:https://financial-transaction/roles}
app.security.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS}                  # SPA origin(s)
```
**Behaviour:**
* `SecurityFilterChain`: stateless, CSRF disabled (bearer-only, no cookies), CORS from properties, `/actuator/health/**` public, other actuator endpoints restricted, `anyRequest().authenticated()`.
* `JwtDecoder`: from `issuer-uri` **plus validators**: `JwtTimestampValidator`, issuer, and an explicit **audience validator** (Spring's default decoder does not check `aud`); clock skew ≤ 60 s; reject tokens missing `sub`.
* `JwtAuthenticationConverter`: authorities `ROLE_<ROLE>` (upper-cased) from the roles claim. Do not map `scope`/`permissions` (D-30). Missing/empty claim → no roles → 403.
* Method security (`@EnableMethodSecurity`): `@PreAuthorize("hasRole('CUSTOMER')")` at the controller/use-case boundary.
* **`CurrentUser` port**: resolves `sub` → `usuario.external_subject` (unique, Flyway `V3`) → the single account (D-25). Unknown `sub` → 403 `{code:"FORBIDDEN"}`; no auto-provisioning (manual/seed linking, D-21).
* **Ownership (authoritative):** every repository access is constrained by the caller's account; foreign resource → 404.
* Errors: 401 (`WWW-Authenticate: Bearer`, `{code:"UNAUTHORIZED",message}`), 403 (`{code:"FORBIDDEN",message}`), same `{code,message}` shape as today.
* Logging: never log tokens; log a hash of `sub` only; add `traceId`.
* Tests/CI use a **mock JWKS issuer** (no Auth0 account, no secrets in CI). A real Auth0 sign-in is a manual check.

**Authorization matrix**
| Endpoint | Required | Ownership rule |
|---|---|---|
| `GET /transactions`, `GET /transactions/{id}` | `ROLE_CUSTOMER` | Caller's account only |
| `POST /transactions/send-pix` | `ROLE_CUSTOMER` | Debits caller's account |
| `POST /transactions/recharge-mobile` | `ROLE_CUSTOMER` | Debits caller's account |
| `POST /transactions/atm-withdrawal` | `ROLE_CUSTOMER` | Debited account must belong to the caller; ledger follows the debited account |
| `POST /transactions/deposit` | `ROLE_CUSTOMER` | Third-party deposits allowed (any existing account) |
| `GET /accounts/me` (AP-8) | `ROLE_CUSTOMER` | Caller's account |
| Actuator (non-health) | network-restricted port | — |

## Frontend specification (Angular 21)
* **Library (D-FE-4):** preferred `@auth0/auth0-angular` (official; standalone `provideAuth0`) **[VERIFY Angular 21 / zoneless compatibility on the day]**; fallback `angular-auth-oidc-client`. Either way wrap it behind an `AuthService` interface in `core/auth` so features never import the SDK.
* **Flow:** `authGuard` (functional `CanActivateFn`) → if no session, remember `returnUrl` and call `loginWithRedirect` with `authorizationParams: { audience, redirect_uri: window.location.origin, scope: 'openid profile email offline_access' }` → return to the app root → SDK exchanges the code → navigate to `returnUrl`.
* **Tokens:** in-memory access token; `useRefreshTokens: true` with rotation (a refresh token stored in memory is lost on reload, then the SDK re-authenticates silently via the Auth0 session). `localstorage` cache is an explicit, recorded trade-off, not a default (XSS exposure).
* **Interceptor:** attach `Authorization: Bearer` only to `API_BASE_URL` requests; 401 → one silent renew attempt, else re-login; 403 → "no permission" page. Writes are never auto-retried (F-21).
* **UI authorization:** `hasRole()` signal only to show/hide; the server is authoritative.
* **Logout:** `logout({ logoutParams: { returnTo: window.location.origin } })` and local state wipe.
* **Runtime config** (`public/config.json`): `domain`, `clientId`, `audience`, `apiBaseUrl`. `clientId` is public; there is no secret.
* **Dev-open mode** (no login, "Demo" banner) remains only for builds with `auth.enabled=false` against the mock issuer.

## End-to-end sequences
1. **Login:** SPA → Auth0 Universal Login (PKCE, audience) → code → token exchange → tokens held per policy.
2. **API call:** bearer → API validates `iss`, `aud`, `exp/nbf`, signature (JWKS cached, refreshed on unknown `kid`) → roles → controller → `CurrentUser` → use case with ownership.
3. **Expiry:** API 401 → SPA silent renew → retry GET once.
4. **Logout:** SPA → Auth0 logout → `returnTo`.

## Free plan limitations & risks **[VERIFY]**
* Free plan monthly-active-user cap is far above the 2–3 users needed here; confirm the current number and terms in the dashboard.
* Free tenants may be inactivity-limited/suspended by the vendor; keep the tenant active before demos.
* Universal Login branding is limited on free plans; custom domain is a paid feature.
* Auth0 rate limits apply to `/oauth/token`; the API never calls Auth0 per request (local JWT validation).
* Pitfalls: issuer **without** trailing slash → 401 on every call; missing `audience` → opaque token → 401; un-namespaced custom claim is silently dropped; Action not attached to the Login flow → no roles claim → 403.
* Single IdP = single point of failure for login (acceptable for a portfolio). Standard OIDC keeps the exit open: replace issuer/audience/clientId (and claim name) to move to Keycloak/Cognito. A local IdP profile for people cloning the repo (Keycloak/Dex in `docker compose`) is an optional later spec, not part of BE-04.

## Security requirements
No client secret in the SPA; PKCE S256; exact callback/logout URL matching; short access-token lifetime; audience validated and never the Management API's; TLS everywhere outside localhost; CORS limited to configured origins; token redaction in logs; secrets (none required) in env.

## Tests (acceptance)
Backend (`spring-security-test` `jwt()` + mock JWKS): no token → 401; expired / wrong `iss` / wrong `aud` → 401; token for the Management API audience → 401; no `customer` role → 403; `sub` not linked → 403; user A on user B's transaction id → 404; CORS preflight only for configured origins; actuator protected. Frontend: guard redirects when unauthenticated; interceptor adds the header only to the API origin; 401 → renew once; logout clears state; one manual sign-in against the real tenant.
Done when: F-01 closed, `BR-04` replaced, all above pass, `git grep` shows no tokens/secrets.

## Implementation tasks (follow `shared/como-executar.md`)
- [x] Human: Auth0 application (SPA) and API created (2026-11-03); record: domain, client id and audience above.
- [ ] Human: Post-Login Action attached to the Login flow; two test users; sign-ups disabled; send each `sub` for seed linking.
- [ ] Backend (BE-04): dependencies, filter chain, decoder + audience validator, role converter, method security, `CurrentUser`, Flyway `V3`, error handlers, CORS, actuator hardening, mock issuer, tests.
- [ ] Contract (BE-06): `bearerAuth` scheme, 401/403 responses.
- [ ] Frontend (FE-03): `core/auth` wrapper, guard, interceptor, logout, config loader.
- [ ] Re-verify every **[VERIFY]** item against current Auth0 docs.

## Skills used
`auth-oidc-config` (deterministic config generation + verification, project skill), `java-modern-engineering` (security as correctness), `java-persistence-performance` (ownership on every query, Flyway for `external_subject`), `openapi-rest-contract` (security scheme), `angular-architect` (guards are UX not security), `angular-module-scaffold` (where auth plugs in), `ui-ux-pro-max` (session-expiry UX).

---
name: auth-oidc-config
description: Configure and verify OAuth2/OIDC authentication (Auth0 today) for a Spring Boot resource server plus an Angular SPA. Hybrid skill - mostly deterministic scripts that validate the provider inputs, generate backend/frontend/portal configuration and verify a real tenant or access token; the model only handles decisions and troubleshooting. Use when setting up, reviewing or debugging Auth0 settings, issuer/audience/roles-claim mismatches, 401/403 after login, or before running the authentication spec. Do not use to write application security code or to create the provider account.
---

# Auth OIDC config (hybrid)

Turns a small, reviewed **inputs file** into every configuration artifact the API, the SPA and the provider portal need, and verifies the result. Scripts do the work that must be exact; you (the model) only decide what scripts cannot.

## Head — what the user must provide

Ask for anything missing **before** running a script. Do not guess values, and never accept secrets.

| # | Input | Where the user gets it | Example | Secret? |
|---|---|---|---|---|
| 1 | `provider` | — | `auth0` (only supported value) | no |
| 2 | `tenantDomain` | Applications > your app > Settings > *Domain* | `dev-abc123.eu.auth0.com` (host only) | no |
| 3 | `spa.clientId` + `spa.applicationType` | Applications > your app > Settings (type must be **Single Page Application**) | `Sy50My7U…` | no (public) |
| 4 | `spa.callbackUrls`, `logoutUrls`, `webOrigins` | decided by the user | `http://localhost:4200` | no |
| 5 | `api.identifier` (+ `api.name`, `signingAlg: RS256`) | Applications > **APIs** > *Create API* (a NEW API, never the built-in Management API) | `https://api.my-product.local` | no |
| 6 | `roles.claim`, `roles.allowed`, `roles.defaultRole` | decided by the user; claim must be a **URL namespace** | `https://my-product/roles`, `["customer"]` | no |
| 7 | `backend.corsOrigins`, `frontend.apiBaseUrl` | decided by the user | `http://localhost:4200`, `http://localhost:8080` | no |
| 8 | `testUsers[].sub` (optional at first) | User Management > Users > *user_id* | `auth0\|65f0…` | no |

**Never ask for or write:** client secret, user passwords, access/refresh/management tokens. If the user pastes one, tell them not to, and do not copy it anywhere.

Start from `examples/inputs.template.json` (blank) or `examples/financial-transaction.inputs.json` (this repo's real, non-secret values).

## Workflow

1. **Collect** the inputs into a JSON file outside source control if they contain anything private, otherwise next to the spec.
2. **Validate:** `node scripts/check-inputs.mjs <inputs.json>` — exit 0 required. It rejects, among others: regular-web-app type, the Management API audience, `https://` in the domain, wildcard/`http` non-localhost URLs, un-namespaced or `auth0.com`-namespaced claims, secret-looking keys. Fix inputs; never bypass.
3. **Generate:** `node scripts/generate.mjs <inputs.json> --out <dir>` writes (refuses to overwrite without `--force`):
   - `backend/application-auth.yaml` and `backend/auth.env.example` — issuer **with trailing slash**, audience, roles claim, CORS
   - `frontend/config.json` — runtime config for the SPA (public values only)
   - `portal/auth0-post-login-roles.js` — the Post-Login Action
   - `portal/portal-checklist.md` — numbered portal steps with the user's own values filled in
   - `portal/expected-token-claims.json` — what a correct access token must contain
4. **Guide the user** through `portal/portal-checklist.md`; for screen-by-screen help and the failure table read [references/auth0-portal-guide.md](references/auth0-portal-guide.md).
5. **Verify the tenant (read-only, public endpoints):** `node scripts/verify-tenant.mjs <inputs.json>` — checks discovery issuer, JWKS and RS256 key.
6. **Verify a real login:** the user obtains an access token and pipes **only the access token** on stdin: `node scripts/decode-token.mjs <inputs.json> < token.txt` (delete the file afterwards). It checks `iss`, `aud` (and not the Management API), `exp`, `sub`, `azp`, roles claim. It does **not** verify the signature; the API does.
7. Report which steps were executed vs only written; record non-secret evidence (e.g. "verify-tenant: 5 OK") where the calling spec asks for it.

Tests of this skill: `node --test scripts/test/auth.test.mjs` (no network, no dependencies).

## What the model decides (the non-deterministic part)

- Whether roles or scopes fit (default: one role claim, no scopes — see `.spec/auth-auth0.md` D-30).
- Which SPA SDK to use (`@auth0/auth0-angular` preferred; check Angular/zoneless compatibility on the day) and token-storage trade-offs.
- Diagnosing failures: map the symptom to the table in the portal guide, then change **one** thing and re-run verification.
- Extending `scripts/lib.mjs` rules and `render.mjs` when the product changes; add a test for every new rule.

## Boundaries

- Does not create the provider account, tenant, application, API, Action or users: the human does that in the portal following the generated checklist.
- Does not write Spring Security or Angular code (use `java-modern-engineering` and `angular-module-scaffold`); it only supplies the configuration they consume.
- Provider support is isolated in `SUPPORTED_PROVIDERS`/`render.mjs`; adding Keycloak/Cognito means new rules + renderers + a guide, with tests.
- Only public tenant endpoints are ever called, with no credentials.

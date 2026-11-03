# Financial Transaction Service (monorepo)

Functional portfolio prototype of a modern online bank (web only, test data).

| Folder | What | CI |
|---|---|---|
| `api-financial-transaction-service/` | Spring Boot API (Pix, deposit, recharge, withdrawal, statement) | `.github/workflows/api-ci.yml` |
| `app-financial-transaction-service/` | Angular web app (created by spec FE-02) | `.github/workflows/app-ci.yml` |
| `prototype/` | Approved UI/UX prototype - visual and flow reference for the frontend | - |
| `.spec/` | SDD knowledge base: `memory/`, `specs/`, execution flow | - |
| `docker-compose.yml` | Local stack (API, PostgreSQL, Kafka, MaiaBank stub) | - |

Each application has its own isolated pipeline (path-filtered) and is built and tested independently.

Run the stack: `docker compose up --build` (API on `localhost:8088`). API details: `api-financial-transaction-service/README.md`. Work is driven by `.spec/spec-execution-flow.md`.

## Authentication (Auth0) - how it works and how to configure it

The API is an OAuth2 **resource server**: it never sees a password. A user signs in at Auth0, gets an **access token** (a signed JWT), and sends it as `Authorization: Bearer <token>`. The API checks the token by itself and then serves only that user's data.

```
User --(1) login page--> Auth0 (e-mail + password are typed at Auth0)
User <-(2) authorization code-- Auth0
App/Postman --(3) code + PKCE verifier--> Auth0 --> access token
App/Postman --(4) GET /api/v1/transactions + Bearer token--> API
API --(5) validates signature, issuer, audience, expiry, role claim
API --(6) token "sub" -> usuario.external_subject -> the customer's account
```

| Term | Meaning here |
|---|---|
| `sub` | The user id inside the token (`auth0\|65f...`). Linked to a customer in `usuario.external_subject`. Not linked = 403. |
| audience | `https://api.financial-transaction.local` - the API identifier; tokens for any other audience are rejected (401). |
| roles claim | `https://financial-transaction/roles` (array), added by a Post-Login Action. Needs `customer` or the API answers 403. |
| SPA client | `Transaction-app`, type *Single Page Application*: Authorization Code + PKCE, **no client secret**. |

Do not use the client-credentials flow (`client_id` + `client_secret`) for this: it issues a machine token with no user and no roles, which the API answers with 403. Never commit, paste or share a client secret; rotate it if it leaked.

### 1. Configure the Auth0 tenant (once)
Dashboard: <https://manage.auth0.com/>. Tenant used by this project: `dev-twljocmfwpvlxx1q.eu.auth0.com`.

1. **Application** (*Applications > Applications > Transaction-app > Settings*)
   - *Application Type* = **Single Page Application**.
   - *Allowed Callback URLs*, *Allowed Logout URLs*, *Allowed Web Origins* = `http://localhost:4200` (add `https://oauth.pstmn.io/v1/callback` to the callback list if you test with Postman). Leave *Cross-Origin Authentication* off and the *Allowed Origins (CORS)* box empty.
   - *Refresh Token Rotation* = on. Click **Save Changes** (nothing is autosaved).
2. **API** (*Applications > APIs > Create API*; never reuse the built-in "Auth0 Management API")
   - Name `Financial Transaction API`, Identifier `https://api.financial-transaction.local` (cannot be changed later), signing algorithm **RS256**.
   - *Settings*: **Allow Offline Access** on; set **Token Expiration** to something short (e.g. 3600 s; the default is 24 h).
   - *Application Access*: authorize `Transaction-app` for user access; client access = none. No permissions/scopes are needed.
3. **Post-Login Action** (*Actions > Library > Create Action > Build from scratch*, trigger *Login / Post Login*) - paste and **Deploy**:
   ```js
   exports.onExecutePostLogin = async (event, api) => {
     const namespace = 'https://financial-transaction/roles';
     api.accessToken.setCustomClaim(namespace, ['customer']);
   };
   ```
   Then *Actions > Triggers > post-login*: drag the Action **into the flow** (between Start and Complete) and click **Apply**. A deployed but unattached Action issues no claim.
4. **Users** (*User Management > Users > Create User*, connection `Username-Password-Authentication`): create at least two test users (the second one proves a customer cannot touch the first one's data). Copy each **user_id** (`auth0|...`). Then *Authentication > Database > Username-Password-Authentication > Settings*: turn **Disable Sign Ups** on.

### 2. Run the API linked to a test user
The local seed creates one demo customer (account `2022`). Link it to the first test user's `user_id`:

```bash
export SPRING_PROFILES_ACTIVE=local
export SEED_OWNER_SUB='auth0|<user_id of the first test user>'
```

Other settings have defaults for this tenant and can be overridden: `AUTH_ISSUER` (with the **trailing slash**), `AUTH_AUDIENCE`, `AUTH_ROLES_CLAIM`, `CORS_ALLOWED_ORIGINS` (see `.env.example`). With `docker compose`, put `SEED_OWNER_SUB` in `.env`.

### 3. Get a user token (Postman)
*Authorization* tab > type **OAuth 2.0** > Grant Type **Authorization Code (With PKCE)**:

| Field | Value |
|---|---|
| Auth URL | `https://dev-twljocmfwpvlxx1q.eu.auth0.com/authorize?audience=https://api.financial-transaction.local` |
| Access Token URL | `https://dev-twljocmfwpvlxx1q.eu.auth0.com/oauth/token` |
| Client ID | the `Transaction-app` Client ID |
| Client Secret | **empty** |
| Callback URL | `https://oauth.pstmn.io/v1/callback` |
| Scope | `openid profile email offline_access` |

Click **Get New Access Token**, sign in as the test user in the Auth0 window, and use the token. The `audience` parameter is mandatory: without it Auth0 returns an opaque token that the API cannot validate.

### 4. Check it works
`GET http://localhost:8080/api/v1/transactions?initial-date=01/01/2000&final-date=31/12/2100` with the Bearer token:

| Status | Meaning | Usual cause |
|---|---|---|
| 200 | Signed in and linked | - |
| 401 | Token rejected | wrong issuer (missing trailing slash), wrong/missing audience, expired token, opaque token |
| 403 | Token valid but not allowed | Action not deployed/attached (no roles claim), or `SEED_OWNER_SUB` differs from the token `sub` |
| 404 on another customer's data | Ownership working | - |

More symptoms and fixes: `.claude/skills/auth-oidc-config/references/auth0-portal-guide.md` (project skill `auth-oidc-config`, which also validates the inputs, generates the configuration files and verifies the tenant and a token). Design and decisions: `.spec/auth-auth0.md`.

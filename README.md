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

## Run back and front together

Everything below starts from the repo root. Ports: Postgres `5433`, Kafka `9092`, Kafdrop `19000`, MaiaBank stub `8089`, API `8088`, app `4200`.

```bash
cp .env.example .env                                                         # once (local, throw-away values)
docker compose up -d pg-db zookeeper kafka kafdrop maiabank-api              # infrastructure only
```

**API** - from the IDE (run configuration with `SPRING_PROFILES_ACTIVE=local`, `SEED_OWNER_SUB`, optional `SEED_SECOND_SUB`) **or** in Docker: `docker compose --profile api up -d --build` (it needs `SEED_OWNER_SUB` / `SEED_SECOND_SUB` in `.env`, the Auth0 `user_id` of your test users).

**App** - `cd app-financial-transaction-service && npm ci && npm start`, then open <http://localhost:4200>. The dev server proxies `/api` to `localhost:8088`.

**Real Auth0 sign-in (manual):** in the Auth0 dashboard (application *Transaction-app*) add `http://localhost:4200` to *Allowed Callback URLs*, *Allowed Logout URLs* and *Allowed Web Origins*. Then click **Access my account** and sign in with a test user. The app reads its Auth0 values from `app-financial-transaction-service/public/config.json` (public values only).

**Automated end-to-end (no password, mock identity provider):**

```bash
docker compose -f docker-compose.yml -f docker-compose.e2e.yml --profile api --profile e2e up -d --build
cd app-financial-transaction-service && npm run e2e:install && npm run e2e   # 3 viewports: 1280 / 768 / 390
```

Details of the e2e setup (mock IdP users, how the browser reaches it): `app-financial-transaction-service/e2e/README.md`.

**Web app in Docker** - `docker compose --profile api --profile web up -d --build` serves the Angular build with nginx on http://localhost:4200 (same origin as `ng serve`, so the Auth0 callback URLs already registered keep working; stop `npm start` first or set `WEB_PORT`). nginx forwards `/api/` to the API container; the public Auth0 values are the build's `config.json`, replaceable by mounting a file over `/usr/share/nginx/html/config.json`.

Shortcut: `docker compose --profile api up --build` runs everything including the API in Docker. API details: `api-financial-transaction-service/README.md`. Work is driven by `.spec/spec-execution-flow.md`.

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
2. **Authorize the app on the API** (*Applications > APIs*) — **without this every sign-in is refused** with `Client "…" is not authorized to access resource server "https://api.financial-transaction.local"`
   1. Open the API whose *Identifier* is `https://api.financial-transaction.local` (name `Financial Transaction API`; create it if it does not exist: signing algorithm **RS256**, *Allow Offline Access* on, a short *Token Expiration* such as 3600 s; never reuse the built-in "Auth0 Management API").
   2. Open the **Application Access** tab (in some dashboards it is called *Machine to Machine Applications*).
   3. Find **Transaction-app** and authorize it. Under *User Access* leave **Authorized** (client access: none; no permissions/scopes are needed).
   4. Save.
   5. Check on the application itself (*Applications > Applications > Transaction-app > Settings*) that `http://localhost:4200` is in **Allowed Callback URLs**, **Allowed Logout URLs** and **Allowed Web Origins**, then **Save Changes**.
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

### Sign-in problems and what they mean
| What you see | Cause | Fix |
|---|---|---|
| Clicking **Sign in** returns to the sign-in page with *"Auth0 said: Client … is not authorized to access resource server …"* | *Transaction-app* is not authorized on the API | Section 1, item 2 above (Application Access > Authorized) |
| *"Callback URL mismatch"* or a CORS/origin error on the Auth0 page | `http://localhost:4200` missing from the application URLs | Section 1, items 1 and 2.5 above |
| After signing in, Home shows **"no access"** and the browser console shows `GET /api/v1/accounts/me 403` | The token is valid but refused. **The API console says which reason** (INFO line): `Access denied (403): the token has no ROLE_CUSTOMER (roles in the token: [...])` = the user has another Auth0 role assigned (the Action then sends that role instead of `customer`): remove it under *User Management > Users > (user) > Roles*, or fix the Action; `Access denied (403): the token subject is not linked to a customer` = the user's `sub` is not `SEED_OWNER_SUB`/`SEED_SECOND_SUB` | Fix as described, then restart the API (the seed relinks on start) and sign in again |
| The sign-in goes straight to Home without a form | An old Auth0 single-sign-on session exists | Not a bug; since 2026-11-04 the app asks for the form every time (`prompt=login`). **Sign out** also ends the Auth0 session |
| After a page reload you must click Sign in again | Tokens are kept in memory only (owner decision) | Expected; one click, no password (the Auth0 cookie signs you in again) |

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

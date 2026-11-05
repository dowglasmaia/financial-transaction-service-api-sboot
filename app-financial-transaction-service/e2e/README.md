# End-to-end tests (Playwright)

They exercise the real chain: **Angular app -> identity provider -> Spring resource server -> PostgreSQL**, on three viewports
(1280, 768, 390), with axe accessibility checks. They never type a real password: sign-in goes through a **mock identity provider**.

## Why a mock IdP of our own
`@auth0/auth0-spa-js` calls fixed Auth0 paths (`/authorize`, `/oauth/token`, `/v2/logout`). A generic mock server does not expose
those, so `e2e/mock-idp/server.mjs` (Node standard library, no dependencies) implements them: discovery, JWKS, Authorization Code
+ PKCE, rotating refresh tokens and logout. The API validates its tokens exactly as it validates Auth0's (issuer, audience, roles
claim). **Never use it outside tests**: anyone can "sign in" as any listed user.

| Mock user | Token subject | What it exercises |
|---|---|---|
| Maria Silva - demo customer | `auth0\|mock-user-1` | linked to account 2022 (balance 5,000.00) |
| João Santos - second customer | `auth0\|mock-user-2` | linked to account 3033 (balance 1,000.00) |
| Carla Souza - not linked | `auth0\|mock-unlinked` | valid sign-in, API answers 403 -> "No access" page |
| Maria Silva - access tokens already expired | `auth0\|mock-user-1` | API answers 401 -> one renewal -> "Session expired" dialog |

## Run
```bash
# from the repo root: the whole stack + the mock IdP (e2e.env points the API at the mock and seeds the two mock customers)
docker compose --env-file .env --env-file e2e.env --profile e2e up -d --build

cd app-financial-transaction-service
npm run e2e:install     # once: downloads Chromium
npm run e2e             # starts `npm start` if it is not already running
```

The browser reaches the mock as `http://mock-idp:9099` (the name the API container uses for the issuer); `playwright.config.ts` maps
that name to `127.0.0.1`. Each test replaces `/config.json` so the app talks to the mock instead of Auth0 (`e2e/support.ts`).

If you ran the stack before with other users, the seed relinks the demo customers to the subjects it is given at start-up; a clean
database is only needed to reset balances (`docker compose down -v`).

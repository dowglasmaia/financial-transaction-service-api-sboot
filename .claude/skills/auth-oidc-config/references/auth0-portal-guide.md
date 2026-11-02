# Auth0 portal guide (screen by screen)

Dashboard: `https://manage.auth0.com/` (pick the right tenant in the top-left switcher). Menu names below are those of the dashboard at the time of writing; re-check if the UI moved. Values in `code` come from your inputs file — `generate.mjs` prints them already filled in `portal-checklist.md`.

## A. Single Page Application
1. **Applications > Applications > Create Application** -> name, choose **Single Page Web Applications** -> Create. (Existing app of the wrong type: *Settings > Application Properties > Application Type* -> change -> Save.)
2. *Settings* tab: copy **Domain** and **Client ID**. Do **not** copy or use the Client Secret.
3. *Application URIs*: set **Allowed Callback URLs**, **Allowed Logout URLs**, **Allowed Web Origins** (exact URLs, comma-separated or one per line, e.g. `http://localhost:4200`). Leave **Allowed Origins (CORS)** and **Cross-Origin Verification Fallback URL** empty (the grey `https://my-app.com` is a placeholder). *Application Login URI* is optional.
4. *Refresh Token Rotation*: **Rotation** ON, reuse interval 0–10 s. *Advanced Settings > Grant Types*: Authorization Code + Refresh Token.
5. **Save Changes** (bottom of the page).

## B. API (this defines the token audience)
1. **Applications > APIs > Create API** (do **not** reuse "Auth0 Management API"; its identifier ends in `/api/v2/`).
2. Name `Financial Transaction API`, Identifier `https://api.financial-transaction.local` (a logical name, need not resolve, **immutable**), Signing Algorithm **RS256**.
3. *Settings*: **Allow Offline Access** ON. *Application Access*: user-delegated = authorize your SPA (or all apps for a single-app tenant); client access = *No apps allowed*.
4. *Permissions*: none needed when authorization uses the role claim.

## C. Post-Login Action (puts roles in the access token)
1. **Actions > Library > Create Action > Build from scratch**: name `add-roles-claim`, trigger **Login / Post Login**.
2. Paste `portal/auth0-post-login-roles.js`, **Deploy**.
3. **Actions > Triggers > post-login**: drag `add-roles-claim` into the flow between *Start* and *Complete*, **Apply**. (Deployed but not attached = no claim = 403 in the API.)

## D. Users and sign-ups
1. **User Management > Users > Create User**: email + password (keep the password to yourself), connection `Username-Password-Authentication`. Create at least two users (one owns the data, one proves access is denied).
2. Open each user: copy **user_id** (`auth0|…`) into `testUsers[].sub`.
3. **Authentication > Database > Username-Password-Authentication > Settings**: **Disable Sign Ups** ON.
4. Optional roles: **User Management > Roles** `customer`, assign to users. Without an assignment the Action falls back to `defaultRole`.

## E. Obtaining a token to verify (no secrets in chat)
Run the SPA and log in, then copy the **access token** from the SPA's own debug output or the network tab (`/oauth/token` response -> `access_token`), put it in a temp file, run `decode-token.mjs`, delete the file. A token with 5 dot-separated parts or one that is not a JWT means the SPA did not send `audience`.

## Symptom -> cause table

| Symptom | Likely cause | Fix |
|---|---|---|
| API 401 on every call, `iss` mismatch in logs | issuer configured without trailing slash | issuer must be `https://<domain>/` |
| API 401, `aud` invalid | SPA not sending `audience`, or API identifier typo/trailing slash | send `audience` on login; make both sides identical |
| Token is not a JWT (opaque) | `audience` missing on `/authorize` | add `authorizationParams.audience` |
| API 403 for a valid login | roles claim absent: Action not deployed/attached, claim not namespaced, or wrong claim name | redo section C, compare with `expected-token-claims.json` |
| Login redirects back with `invalid redirect_uri` / "Callback URL mismatch" | URL not in Allowed Callback URLs (scheme, port or slash differ) | add the exact URL, Save |
| Silent renew fails / login loop after reload | Allowed Web Origins missing; third-party cookies blocked | add origin; use refresh-token rotation |
| `Service not found: <audience>` on login | API identifier in the SPA differs from the one created | use the API's exact identifier |
| Sign-up form appears | Sign Ups not disabled | section D.3 |
| Browser CORS error on the API | `corsOrigins` lacks the SPA origin (not an Auth0 setting) | fix `CORS_ALLOWED_ORIGINS` |
| Works locally, fails deployed | callback/logout/web-origin lists lack the deployed https URL | add it to all three |

## Security reminders
Public SPA = no client secret, PKCE only. Keep access-token lifetime short (default 24 h on new APIs is too long: set *API > Settings > Token Expiration* to ~15–60 min). The Management API audience must never be accepted by the backend. Re-verify these UI paths against the Auth0 docs when something looks different.

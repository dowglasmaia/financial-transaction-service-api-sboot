# SPEC-FE-03 — Auth, Home, Statement and detail

**Status:** Draft (not authorized) · **Depends on:** FE-02, BE-04, BE-06 · **Parallel with:** FE-04 (after FE-03 auth lands)
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md` (incl. *Frontend agent autonomy*) and the visual/flow reference `../../prototype/Maia Bank UI prototype/` (open `Maia Bank.dc.html`). Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Implement sign-in, Home and the Statement/detail from the approved design and contract.

## Scope
- Auth: `AuthService` wrapper (Auth0/OIDC code + PKCE, `audience` always sent), guard, callback, interceptor, silent renew, session-expired dialog (see `auth-auth0.md` frontend section)
- Home: balance from `/accounts/me`, period totals + chart (table fallback), recent activity, quick actions, AI Assistant concept card
- Statement: period/type filters in query params, server pagination, sort, table/cards, detail by id, all states

**Out of scope:** Operations (FE-04)

## Tasks
- [ ] Module scaffold + `doc.md` per feature (`angular-module-scaffold`)
- [ ] Auth flow + tests
- [ ] Home
- [ ] Statement + detail
- [ ] AI Assistant concept screen (static, Coming soon)
- [ ] a11y + responsive checks

## Acceptance
- [ ] Unauthenticated routes redirect to login; 401 → one renew then login
- [ ] UI never computes balance; totals come from the backend
- [ ] Single-day filter works end-to-end

## Minimal validation (required to close this spec)
- [ ] e2e with the mock issuer: unauthenticated route redirects to login; after login Home shows the balance from `/accounts/me`
- [ ] Statement: filter a single day, see rows, open detail by id
- [ ] No tokens in URLs/console; axe has no serious violations on these screens

## Stop and ask
- OIDC library choice and token storage

## Skills
angular-module-scaffold, angular-tailwind-ui-specialist, ui-ux-pro-max

## Result
_Filled on completion: result, checks run, accepted limitations._

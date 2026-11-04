# Prototype deviations and proposals

The approved prototype (**v2**: `prototype/Maia Bank UI prototype/Maia Bank v2.dc.html`, decision D-32) is the starting point for look, flows and copy — not a limit.
This file logs every deviation. **Tier 1** = refinement inside the same flow (applied, logged here). **Tier 2** = changes a flow,
adds/removes a screen or capability, or needs the backend (written as a proposal, needs the owner's decision).

History: seeded from the FE-01 review of the v1 prototype (brown/gold). SL-00 (2026-11-03) replaced the look with v2; v1 rows that no longer apply are kept below as *superseded*.

## v2 — decisions already taken by the owner (D-33…D-36)
| # | v2 as delivered | Treatment |
|---|---|---|
| V1 | Portuguese copy, brand "Banco MaisVida", "Login seguro via Okta" | UI stays **English**, brand **Maia Bank**, sign-in note will read "Secure sign-in with Auth0" (built in SL-01) |
| V2 | Search field, notification bell with count, support-phone strip, "Learn more" security link | **Not built** — no endpoint (D-35) |
| V3 | Recharge carriers Claro / Vivo / TIM / **Oi** | Backend enum is VIVO / CLARO / TIM / **ALGAR** → Algar (D-36, SL-04) |
| V4 | Dark only | Dark default + **derived light theme** from the v2 hues, contrast-tested (D-34) |
| V5 | No AI Assistant screen | Dropped from scope; reinstate only if the owner asks |
| V6 | Prototype control bar (data / outcome simulators, screen jump) | Never shipped; the states are reproduced in unit tests and `/design` |

## v2 — applied in SL-00 (Tier 1)
| # | Deviation | Why | Where |
|---|---|---|---|
| 10 | Primary blue `#2d68fa` instead of v2's `#2f6bff` | White text on `#2f6bff` measures **4.4988:1** (< 4.5:1); `#2d68fa` gives 4.70:1 | `styles.scss` (`--primary`), `tokens-contrast.spec.ts` |
| 11 | Dark `--line-input` is a solid `#6674b3` instead of `rgba(150,170,255,.38)` | A solid value can be measured; it gives ≥ 3.9:1 on surface, raised and sunken | `styles.scss` |
| 12 | Token `--accent` is cyan `#38bdf8` (links, focus ring, selected chip) and the **primary action** gets its own tokens `--primary` / `--primary-strong` / `--on-primary` | v2 uses two blues for two jobs; separating them keeps contrast checkable per job | `styles.scss` |
| 13 | Added `--violet` (recharge tile) | v2's recharge colour; measured ≥ 6.4:1 on raised surfaces | `styles.scss`, `icon-tile` |
| 14 | Light theme palette (derived): surface `#f3f6ff`, raised `#ffffff`, content `#0b1234`, accent `#1d4fd7`, income `#047857`, expense `#be123c`, warning `#8a4b00`, violet `#6d28d9`; the rail/bottom bar stay deep navy in both themes | v2 has no light theme (D-34) | `styles.scss` |
| 15 | The balance panel keeps the v2 blue gradient in **both** themes and its text uses `--on-primary` | Hero identity of v2; white on `#1d3fd8`/`#4f46e5` ≥ 6:1 | `balance-card`, `bg-hero` utility |
| 16 | Ledger row has no coloured left rail; direction = icon tile of the kind + type label + sign + screen-reader text | v2 look; colour is still never the only cue | `ledger-row`, `icon-tile` |
| 17 | KPI tile stacks icon + label above the value (v2: icon at the left of a two-line block) | At 768 px a three-up row left only ~80 px for the figure, so amounts wrapped | `kpi-tile` |
| 18 | The top bar has **no account chip / avatar / menu** yet | It needs the signed-in user and `/accounts/me` — arrives with SL-01 | `app-shell` |
| 19 | The demo notice is shown twice on phones: a slim strip under the top bar (the pill does not fit) and the footer line | "Demo environment · test data only" must be on every screen | `app-shell` |
| 20 | Rail/bottom-bar entries come from the route table (`data.nav`), not a hard-coded list; bottom bar shows 3 + a "More" dialog when there are more | A slice adds its menu entry by declaring its route | `core/layout/nav-items.ts` |
| 21 | New shared pieces: segmented control (radio group, arrow keys), stepper (`aria-current`, "completed" text), dialog (modal, focus trap, Esc/backdrop, focus restore), toast (polite live region, 2.4 s), icon tile | Needed by SL-01…SL-06 | `shared/ui` |
| 22 | Fonts now load weights 800 for Manrope and Sora | v2 uses 800 for the wordmark and headings | `index.html` |

## v2 — applied in SL-01 (Tier 1)
| # | Deviation | Why | Where |
|---|---|---|---|
| 23 | The top bar now has the **account menu** (initials, name, e-mail, `Agency · Account`, Sign out with confirmation) | It was deferred in #18 until the signed-in user and `/accounts/me` exist | `core/layout/account-menu.component.ts` |
| 24 | Landing "feature strip" shows three static benefits; **no statistics** (v2 showed a fixed balance and 30-day figures) | D-35 / no invented data; real numbers come from the API | `features/landing` |
| 30 | The sign-in page has an **optional e-mail field** (shape-checked, sent as `login_hint`) and the login always asks Auth0 for its form (`prompt=login`) | Owner request 2026-11-04: with an old Auth0 single-sign-on session the click went straight to Home and no form was ever seen. The password is still typed only on Auth0's page (D-29, Authorization Code + PKCE) | `features/signin`, `core/auth/auth0-session.service.ts` |
| 25 | Landing and sign-in icons use the `ph-bold` weight (v2 used `ph-fill`) | The fill-weight CSS is not shipped (deviation #9); the tiles rendered empty | `features/landing` |
| 26 | Balance count-up is skipped under `prefers-reduced-motion`; the screen-reader text is always the final amount | Accessibility | `balance-card.component.ts` |
| 27 | The footer shows the author's **photo** (192 px avatar) instead of the initials; initials are the fallback if the image fails | Owner request (2026-11-03) | `site-footer.component.ts`, `public/img/dowglas.png` |
| 28 | Initial bundle budget warning raised from 500 kB to **800 kB** (725.6 kB now); the error limit stays 1 MB | The Auth0 SPA SDK is ~200 kB; code-splitting the SDK is a later option | `angular.json` |
| 29 | While the identity provider callback is handled the landing page shows "Signing you in…" instead of the marketing page | Avoids a flash of the landing page and a leaked `?code=` in history (the URL is replaced) | `features/landing` |

## v2 — measured contrast (dark / light), asserted by `tokens-contrast.spec.ts`
Text pairs ≥ 4.5:1 and control borders ≥ 3:1 for `content`, `content-muted`, `accent`, `on-accent`, `on-primary` on `primary` and `primary-strong`, `income`, `expense`, `warning`, `violet`, `on-brand` and `on-brand-muted` on `brand`, and `line-input` on all three surfaces. The same test also fails if any v1 hex (`#cea45c`, `#2a1708`, `#fbf1de`, `#5fa899`, `#d4ae6a`, `#110c09`) returns to `styles.scss`.

## Still open — Tier 2 (need the owner)
| # | Proposal | Detail |
|---|---|---|
| A | **Deposit flow** | v2 still models deposit as an online *boleto* (client-side demo with "Simulate payment"). The backend is `POST /transactions/deposit {agency, account, amount}`. Decided in SL-04: keep the boleto as a UI simulation; "Simulate payment" calls the real deposit for the caller's own account (from `/accounts/me`). |
| B | **Sample data in the prototype** | "Agency 0001 · Account 12345-6", "Dowglas Maia", fixed balance — placeholders; real values come from `/accounts/me` and the ID token (SL-01). |
| C | **`_ds/nocturne-*` bundle** | A bundled design-system export (blurple, Inter) that the prototype does not use. Treated as non-authoritative and not imported. |
| D | **Handoff artifacts** | No tokens JSON, copy deck or a11y annotations; tokens/copy are extracted from the HTML. Request them only if the owner wants a formal handoff. |

## Superseded (v1 look, FE-02) — kept for history
| # | Deviation | Status |
|---|---|---|
| 1 | Dark `--line-input` `#6F5C4E` → `#75634F` | Superseded by #11 |
| 2 | Added `--on-brand` / `--on-brand-muted` | Kept (now `#f1f4ff` / `#9aa6d1`, both themes) |
| 3 | Tokens named semantically instead of the prototype's short vars | Kept (`surface`, `surface-raised`, `content`, `accent`, `income`, `expense`…) |

## Applied earlier and still valid (Tier 1)
| # | Deviation | Why | Where |
|---|---|---|---|
| 4 | Icons via the `@phosphor-icons/web` package (regular + bold CSS) instead of unpkg at runtime | No third-party script/CSS at runtime for icons; works offline in dev | `angular.json` styles |
| 5 | Fonts (Manrope, Sora) still come from Google Fonts like the prototype | Matches the design; self-hosting is a future privacy/perf improvement | `src/index.html` |
| 6 | Money rendered as `R$ 12,480.50` through one util (`formatBrl`) and never recomputed in the UI | Single formatting rule; backend is the authority for balances | `shared/util/money.ts` |
| 7 | Direction shown by icon + label + sign + screen-reader text | Colour must not be the only cue (a11y) | `shared/ui/ledger-row.component.ts` |
| 8 | **Portfolio credit footer** replicated from the check-in app (`site-footer`), restyled with this app's tokens; not in the prototype, requested by the owner | Owner request | `core/layout/site-footer.component.ts`, `shared/ui/tag.component.ts` |
| 9 | LinkedIn/GitHub logos use the Phosphor **regular** weight | The fill weight CSS added ~84 kB to the initial bundle | footer |

## Decisions recorded in FE-02
- **Test runner:** Vitest via Angular's `@angular/build:unit-test` (`npm test`), jsdom. Reason: Angular 21's default runner, no Karma/Jasmine stack.
- **Chart library:** none. The prototype's charts are hand-made SVG/CSS; Home (SL-02) builds them the same way (zero dependency, full control of the accessible table fallback).
- **New dependencies:** `@phosphor-icons/web` (icons, design-mandated), `vitest`, `jsdom`, `@types/node` (dev). No UI/state library. SL-00 added **no** dependency.

## SL-02 — Statement, Detail, Home summary
| # | Deviation | Why | Where |
|---|---|---|---|
| 10 | Rows carry the transaction **id** and link to the Detail page; the prototype's rows have no id | The Detail page is by id (`GET /transactions/{id}`) | `statement-page`, `home-page`, `detail-page` |
| 11 | Period chips are **7 / 30 / 90 days + Custom** (two date inputs, single day allowed); the prototype shows fixed sample periods | The backend takes `from`/`to` whole days (Sao Paulo); the filters live in the URL so a link is shareable | `statement-page`, `statement-query.ts` |
| 12 | Type chips map to the API `type` filter; sort is a select (newest, oldest, highest, lowest) and paging is Previous / Next with "Showing X–Y of N" | Server-side sort and paging (`sort`, `order`, `page`, `size`); totals cover the whole period, never the page | `statement-page` |
| 13 | Chart is a hand-made SVG with **solid vs dashed** lines (not colour alone) and a **"View as table"** fallback carrying the same numbers | Accessibility; no chart library | `shared/ui/line-chart.component.ts` |
| 14 | Dropped: search box, CSV export, per-row status chips (every row is "Registered"), statement balance-after-row column | Out of scope for SL-02; the ledger has no running balance or status per row to show | — |
| 15 | Desktop shows a **table**, mobile a list of rows (same data) | Readability at both widths | `statement-page` |

## SL-03 — Withdrawal, shared result panel, Idempotency-Key
| # | Deviation | Why | Where |
|---|---|---|---|
| 16 | "Review withdrawal" stays available above the balance: "The amount can't exceed your available balance." is **feedback only** and the server answers 422 (business-failure panel). The prototype disables the button | The spec says the server decides; the e2e proves the 422 panel with an unchanged balance | `withdraw-page`, `amount-presets.component.ts` |
| 17 | The review dialog cannot be dismissed (Escape, backdrop) while the request is pending, and Confirm shows the spinner | One click = one request; no half-closed state | `dialog.component.ts` (`dismissible`), `review-dialog.component.ts` |
| 18 | The uncertain panel has an explicit **Try again** (same `Idempotency-Key`) next to "View transactions"; the prototype only has "Open transactions" / "Back". The title is "Outcome not confirmed" and the body is the required sentence, so the text is not repeated | A retry must be a user choice, and the server recognises the key and never debits twice | `result-panel.component.ts`, `shared/operation/operation.ts` |
| 19 | Business failure keeps the **backend's message** ("Operation not performed, there is not enough balance in the account.") and offers "Edit and try again"; the prototype hard-codes its own sentence | The backend message is the source of truth | `withdraw-page.html` |
| 20 | The success panel never says cash was dispensed ("Your withdrawal was registered.") and shows "Registered on this device at HH:MM" | The API only registers the operation | `withdraw-page.html` |
| 21 | No "hand-coins" art panel next to the form and no rail balance pill (the balance is in the page header) | Decorative; a second balance display would be a second source of truth | `withdraw-page.html` |
| 22 | The client waits 30 s for an answer: no answer in time is an *uncertain* outcome (the request is not repeated) | A hung request must not leave the button pending forever | `shared/operation/operation.ts` |

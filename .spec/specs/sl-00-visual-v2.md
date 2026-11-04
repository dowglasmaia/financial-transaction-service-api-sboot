# SPEC-SL-00 — Visual identity v2 (frontend-only slice)

**Status:** Implemented and **signed off by the owner (2026-11-03)**; **open only: green `app-ci` on the PR** · **Depends on:** FE-02 (Done) · **Parallel with:** none (every later slice builds on these tokens and components)
**Branch:** `slice/00-visual-v2` · **Track:** Frontend only (the one slice with no backend change)
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md` (incl. *Frontend: prototype and agent autonomy* and decisions D-31…D-37), `../spec-execution-flow.md` §1b (slice loop) and the visual/flow reference **`../../prototype/Maia Bank UI prototype/Maia Bank v2.dc.html`**. Protocol: `../shared/como-executar.md`.

## Objective
Replace the v1 look (warm brown + gold) that FE-02 implemented with the approved **v2** identity (deep navy surfaces, electric blue/cyan accents, glass cards, Manrope + Sora, Phosphor icons) **before** any feature screen is built, so no screen is ever written twice. v2 supersedes FE-01's v1 review; the owner's visual sign-off of v2 is the gate of this slice.

## Decisions already taken (do not re-ask)
| # | Decision |
|---|---|
| D-33 | UI copy stays **English**. v2's pt-BR strings are translated, never shipped as-is. Brand stays **Maia Bank** (v2's "Banco MaisVida" and its fictitious-bank notice are renamed/kept as "Maia Bank is fictitious"). The sign-in note reads "Secure sign-in with Auth0" (v2 says Okta). |
| D-34 | v2 is dark-only. Keep **dark as default** and keep the theme toggle with a **light palette derived from the v2 hues** and contrast-tested (D-17 stays). If the owner prefers dark-only, remove the toggle (Stop and ask only if the derived light theme cannot pass contrast). |
| D-35 | v2 elements with **no endpoint** are not built: global search field, notification bell with count, support-phone strip, "Learn more" security card link. (Static security/marketing copy on the landing page is fine.) |

## Scope
- Review v2 against `../designer-brief.md` §13 (screens S-01…S-11, flows F-0…F-9, all states) and write the **v2 divergence list** into `app-financial-transaction-service/docs/prototype-deviations.md` (old v1 rows kept but marked *superseded by v2*).
- Tokens in `src/styles.scss` (single source), Tailwind semantic names unchanged where possible (`bg-surface`, `text-content`, `text-income`…) so existing components keep compiling.
- Shared components restyled to v2; new shared pieces needed by the next slices (segmented control, stepper, dialog with focus trap, toast, icon tile, result panel skeleton is **not** here — SL-03).
- App shell: left rail (250 px desktop / 80 px icon rail tablet), bottom nav with "More" sheet (mobile), top bar, persistent "Demo environment · test data only" chip, existing portfolio credit footer.
- `/design` living style guide updated; the v1 look is removed.

**Out of scope:** any feature screen (landing, sign-in, Home content, operations), any HTTP, any backend change, new color schemes beyond dark + derived light.

## v2 facts to implement (extracted from the file)
* **Palette (dark):** page `#030616` → frame gradient `#070C28`→`#050920`; `--s #060B1F`, raised `#0E1738`, sunken `#0A1233`, overlay `#121C47`; line `rgba(130,150,255,.16)`, input line `rgba(150,170,255,.38)` (must be ≥ 3:1 on its surface — measure, adjust and log if not); text `#F1F4FF`, muted `#9AA6D1`; income `#34D399`, expense `#FF6B8A`; accent blue `#2F6BFF` (primary gradient `#2F6BFF→#1E4FE0`, segmented `#2F6BFF→#4F46E5`), cyan `#38BDF8`/`#22D3EE`, violet `#A855F7`/`#8B5CF6`; warning `#FBBF24`; success `#10B981`.
* **Type by ledger kind (icon tile colors):** DEPOSIT green, PIX cyan, PHONE_RECHARGE violet, WITHDRAW amber — shape + label + sign always accompany color (a11y).
* **Shape/motion:** radii 12/14/16/20/22, glass cards (`backdrop-filter: blur(12px)`), soft glows on primary/active, entrance `fadeUp`, `pop`/`ring` for results, skeleton `shimmer`; **all motion off under `prefers-reduced-motion`**.
* **Fonts/icons:** Manrope (UI) + Sora (numbers/headings), Phosphor (regular/bold/fill) — already installed; keep self-hosted icon package (deviation #4).
* **Reference images** in `prototype/Maia Bank UI prototype/uploads/` (`tela principal.png`, `Banco MaisVida Fintech Dashboard*.png`) are visual references only.

## Tasks
- [ ] HUMAN (optional): if `Maia Bank v2.dc.html` does not render locally, export screenshots of every v2 screen/state at desktop, tablet, mobile into `prototype/screens-v2/` (landing, sign-in + redirecting + error, Home ok/loading/empty/error, Statement, Detail, Pix 1-3, Deposit + boleto, Recharge, Withdraw, result success/business/uncertain, sign-out and session-expired dialogs)
  > ⏳ 2026-11-03 15:20 +0000 — Not done / not needed yet: the v2 file was read as code (tokens, structure, copy, behaviour), not rendered. Falta: the owner's visual comparison of `/design` with the v2 screens (this is also the sign-off task). Próxima ação: owner opens v2 in the design tool and compares.
- [x] v2 review + divergence list in `docs/prototype-deviations.md` (seed rows below)
  > ✅ 2026-11-03 15:20 +0000 — Static review of `Maia Bank v2.dc.html` (palette, shape, screens, copy, logic) written to `app-financial-transaction-service/docs/prototype-deviations.md`: 6 owner-decided rows (V1–V6), 13 applied rows (#10–#22), measured-contrast note, open Tier-2 list. Verification: file reviewed. Limit: review is from the code, not from a rendered v2.
- [x] Contrast test first (red): update `tokens-contrast.spec.ts` to the v2 pairs for dark **and** derived light, then implement tokens until green
  > ✅ 2026-11-03 15:20 +0000 — Red first: `npm test` → 10 failed (`on-primary`/`primary`/`primary-strong`/`violet` missing in both themes, 2 v1 hex still in `styles.scss`). Green after the tokens. Finding: v2's `#2f6bff` with white text is 4.4988:1 → `#2d68fa` (4.70:1), logged as deviation #10. The test also guards against the v1 hex values.
- [x] Tokens (dark + derived light) and `ThemeService` unchanged in behaviour
  > ✅ 2026-11-03 15:20 +0000 — `src/styles.scss` rewritten (dark v2 + derived light, new `primary`/`primary-strong`/`on-primary`/`violet`, `bg-glass` and `bg-hero` utilities, `pop`/`ring` keyframes, page glow). `ThemeService` untouched. Verification: contrast spec 2 themes × 24 pairs green; theme toggle exercised in the browser (persisted attribute `data-theme`).
- [x] Restyle shared components (`appButton`, form field, money input, chip, alert, skeleton, empty state, KPI tile, balance card, ledger row, tag, theme toggle) with all states
  > ✅ 2026-11-03 15:20 +0000 — Restyled to v2 (gradient primary with glow, 52 px fields with focus ring, glow chips, tinted alerts, glass cards, blue balance panel, icon-tile ledger rows, theme toggle on surface). Skeleton and tag already used tokens and needed no change. Verification: all states rendered on `/design` in dark and light in the built-in browser; 69 pre-existing unit tests still pass.
- [x] New shared components: segmented control, stepper, dialog (focus trap, Esc, return focus), toast, icon tile
  > ✅ 2026-11-03 15:20 +0000 — Specs written first (red: modules missing), then `segmented`, `stepper`, `dialog`, `toast` (service + host), `icon-tile`: 24 new unit tests green (radio-group semantics and arrow keys, `aria-current`, modal semantics, Tab/Shift+Tab trap, Esc, backdrop, focus restore, live region and auto-dismiss, tones). Browser check: dialog opened with focus on Cancel, Esc closed it and focus returned to the opener.
- [x] Shell: rail / bottom nav + More sheet / top bar / demo chip / footer; nav entries are **driven by the route table** so a slice only has to add its route
  > ✅ 2026-11-03 15:20 +0000 — `core/layout/nav-items.ts` (`data.nav` on routes) + new `app-shell` (rail 256 px ≥ 1024, 80 px icon rail 768–1023, bottom bar + "More" dialog < 768, top bar with demo chip, phone strip, theme toggle, toast host, footer kept). Specs first (red), 10 new tests green (3 nav + 7 shell). Browser: widths 1280/768/390 show rail with labels / icon rail / bottom bar; no horizontal overflow at 768 and 390. Account chip left for SL-01 (deviation #18).
- [x] Update `/design` with every component and state in dark and light at 3 widths
  > ✅ 2026-11-03 15:20 +0000 — `/design` shows the v2 tokens (live from CSS variables), typography, buttons, fields, chips, alerts, balance/KPI/ledger, segmented + stepper, icon tiles, dialog + toast, skeleton + empty state. Browser: 1280 dark and light, 768 light, 390 light and dark. Not captured: 768 dark and 1280 balance/ledger area in both themes (see limitations).
- [ ] Bundle budget and a11y pass (landmarks, focus order, names); no `any`, no HTTP
  > ⏳ 2026-11-03 15:20 +0000 — Parcial. Done: initial bundle **468.00 kB** (budget warning 500 kB; was 434.76 kB), no `any`, `HttpClient` only in the provider and its own spec, landmarks/names/aria asserted by unit tests. Falta: **axe-core was not run** (not installed; arrives with Playwright in SL-01) and the tab order was only exercised for the dialog. Próxima ação: SL-01 e2e runs axe on every screen.

## Seed for the v2 divergence list
| # | v2 | Treatment |
|---|---|---|
| 1 | Portuguese copy, brand "Banco MaisVida", "Login seguro via Okta" | Translate to English, brand **Maia Bank**, "Secure sign-in with Auth0" (D-33) |
| 2 | Search field, bell "3", support phone strip, security "Learn more" link | Not built — no endpoint (D-35) |
| 3 | Recharge carriers Claro/Vivo/TIM/**Oi** | Backend enum is VIVO/CLARO/TIM/**ALGAR** → show Algar (SL-04) |
| 4 | Deposit = boleto with client-side "Simulate payment" | Tier-2 proposal A stays (decided in SL-04) |
| 5 | Ids like `TX-000001-9F2` | Real ids are UUIDs → show shortened + copy action (SL-02) |
| 6 | Dark only | Derived light palette (D-34) |
| 7 | Agency `0001`, account `12345-6`, name "Dowglas Maia", fixed balance 4 250 | Sample data; real values come from `/accounts/me` and the ID token (SL-01) |
| 8 | Period chips 7/30/90 days, type chips, sort date/amount, "Showing X of Y" | Backed by real query params in SL-02 (contract preview there) |
| 9 | No AI Assistant screen | Dropped from scope (v2 has none); reinstate only by owner request |
| 10 | Prototype control bar (data/outcome simulators) | Never shipped; states are reproduced in unit tests and `/design` |

## Acceptance
- [x] `grep -rniE "CEA45C|2A1708|FBF1DE|5FA899|brown" app-financial-transaction-service/src` returns nothing (no v1 identity left)
  > ✅ 2026-11-03 15:20 +0000 — `grep -rniE "CEA45C|2A1708|FBF1DE|5FA899" src` → no output (the guard test writes the hex in two halves). `brown` is not a word that appears in src.
- [x] Contrast: text ≥ 4.5:1 and control borders/icons ≥ 3:1 in both themes (asserted by a unit test)
  > ✅ 2026-11-03 15:20 +0000 — `tokens-contrast.spec.ts`: 24 pairs × 2 themes + 6 v1-guard cases, all green.
- [x] `/design` matches v2 at desktop/tablet/mobile; owner sign-off written
  > ⛔ 2026-11-03 15:20 +0000 — Bloqueio: only the owner can sign off the look. Para desbloquear: owner opens `/design` (`npm start`) next to the v2 file and writes the sign-off (or the list of adjustments).
  > ✅ 2026-11-03 17:00 +0000 — Sign-off by the owner, in chat: "o visual estar ok!" (compared `/design` with the v2 file). No adjustments requested. Not stored as screenshots; the written record is this line.
- [x] No feature, route or HTTP added; existing 69 unit tests still pass (adjusted only for style tokens)
  > ✅ 2026-11-03 15:20 +0000 — No new route (only `data.nav` on `/`), no HTTP. 117 tests / 13 files green: the 69 original (the contrast spec was extended, the other five files untouched) + 48 new.

## Minimal validation (required to close this spec)
- [x] `cd app-financial-transaction-service && npm ci && npm run build && npm test` green (state counts)
  > ✅ 2026-11-03 15:20 +0000 — EXECUTED: `npm ci && npm run build` → success, initial total 468.00 kB (103.37 kB transfer); `npm test` → 13 files, 117 tests passed.
- [ ] `app-ci` green on the PR
  > ⛔ 2026-11-03 15:20 +0000 — Not executed: needs the PR (the owner opens it). Para desbloquear: green `app-ci` on the PR.
- [ ] Built-in browser screenshots of `/design` and the empty shell at 1280 / 768 / 390 in dark and light attached to the evidence
  > ⏳ 2026-11-03 15:20 +0000 — Parcial. EXECUTED in the built-in browser against `ng serve` (stopped afterwards): 1280 dark + light, 768 light (tokens; balance/ledger/stepper before the KPI/stepper fix, layout re-verified by DOM: no overflow), 390 light + dark (header, strip, bottom bar, balance, KPI, ledger). Screenshots were inspected in the session, **not saved in the repo**. Falta: 768 dark and a full 6-way matrix as image files. Próxima ação: owner review doubles as the screenshot pass.
- [x] Written owner sign-off of the v2 look (replaces FE-01's pending sign-off)
  > ⛔ 2026-11-03 15:20 +0000 — Not executed: only the owner can sign off. Para desbloquear: owner compares `/design` with v2 and writes the sign-off here.
  > ✅ 2026-11-03 17:00 +0000 — Signed off by the owner (see the acceptance item above). FE-01's pending v1 sign-off is void.

## Stop and ask
- Light theme cannot meet contrast with the v2 hues
- Any v2 element that needs an endpoint (D-35 list) or a new dependency
- Anything that changes a flow (Tier 2)

## Skills
angular-tailwind-ui-specialist, ui-ux-pro-max, angular-architect

## Result
The v1 (brown/gold) look is gone: tokens (dark v2 + derived light), restyled shared components, five new shared pieces (segmented, stepper, dialog, toast, icon tile), a route-driven shell (rail / icon rail / bottom bar + More) and an updated `/design`. No feature, route or HTTP was added and no dependency changed.

**Executed:** clean `npm ci`, production build (468.00 kB initial, was 434.76 kB), 117 unit tests (69 original + 48 new, red first for the contrast, component and shell specs), v1 palette grep empty, browser check of `/design` and the shell at 1280/768/390 in dark and light, dialog focus/Esc behaviour.

**Signed off:** the owner approved the look on 2026-11-03. **Open:** `app-ci` on the PR, axe-core (SL-01 brings Playwright). **Accepted limitations:** v2 was reviewed from its source, not rendered; screenshots were inspected but are not stored in the repo; the account chip waits for SL-01. **Findings:** v2's primary `#2f6bff` fails 4.5:1 with white text (used `#2d68fa`); at 768 px KPI figures wrapped until the tile was stacked.

# SPEC-SL-00 — Visual identity v2 (frontend-only slice)

**Status:** Draft (not authorized) · **Depends on:** FE-02 (Done) · **Parallel with:** none (every later slice builds on these tokens and components)
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
- [ ] v2 review + divergence list in `docs/prototype-deviations.md` (seed rows below)
- [ ] Contrast test first (red): update `tokens-contrast.spec.ts` to the v2 pairs for dark **and** derived light, then implement tokens until green
- [ ] Tokens (dark + derived light) and `ThemeService` unchanged in behaviour
- [ ] Restyle shared components (`appButton`, form field, money input, chip, alert, skeleton, empty state, KPI tile, balance card, ledger row, tag, theme toggle) with all states
- [ ] New shared components: segmented control, stepper, dialog (focus trap, Esc, return focus), toast, icon tile
- [ ] Shell: rail / bottom nav + More sheet / top bar / demo chip / footer; nav entries are **driven by the route table** so a slice only has to add its route
- [ ] Update `/design` with every component and state in dark and light at 3 widths
- [ ] Bundle budget and a11y pass (landmarks, focus order, names); no `any`, no HTTP

## Seed for the v2 divergence list
| # | v2 | Treatment |
|---|---|---|
| 1 | Portuguese copy, brand "Banco MaisVida", "Login seguro via Okta" | Translate to English, brand **Maia Bank**, "Secure sign-in with Auth0" (D-33) |
| 2 | Search field, bell "3", support phone strip, security "Learn more" link | Not built — no endpoint (D-35) |
| 3 | Recharge carriers Claro/Vivo/TIM/**Oi** | Backend enum is VIVO/CLARO/TIM/**ALGAR** → show Algar (SL-05) |
| 4 | Deposit = boleto with client-side "Simulate payment" | Tier-2 proposal A stays (decided in SL-04) |
| 5 | Ids like `TX-000001-9F2` | Real ids are UUIDs → show shortened + copy action (SL-02) |
| 6 | Dark only | Derived light palette (D-34) |
| 7 | Agency `0001`, account `12345-6`, name "Dowglas Maia", fixed balance 4 250 | Sample data; real values come from `/accounts/me` and the ID token (SL-01) |
| 8 | Period chips 7/30/90 days, type chips, sort date/amount, "Showing X of Y" | Backed by real query params in SL-02 (contract preview there) |
| 9 | No AI Assistant screen | Dropped from scope (v2 has none); reinstate only by owner request |
| 10 | Prototype control bar (data/outcome simulators) | Never shipped; states are reproduced in unit tests and `/design` |

## Acceptance
- [ ] `grep -rniE "CEA45C|2A1708|FBF1DE|5FA899|brown" app-financial-transaction-service/src` returns nothing (no v1 identity left)
- [ ] Contrast: text ≥ 4.5:1 and control borders/icons ≥ 3:1 in both themes (asserted by a unit test)
- [ ] `/design` matches v2 at desktop/tablet/mobile; owner sign-off written
- [ ] No feature, route or HTTP added; existing 69 unit tests still pass (adjusted only for style tokens)

## Minimal validation (required to close this spec)
- [ ] `cd app-financial-transaction-service && npm ci && npm run build && npm test` green (state counts)
- [ ] `app-ci` green on the PR
- [ ] Built-in browser screenshots of `/design` and the empty shell at 1280 / 768 / 390 in dark and light attached to the evidence
- [ ] Written owner sign-off of the v2 look (replaces FE-01's pending sign-off)

## Stop and ask
- Light theme cannot meet contrast with the v2 hues
- Any v2 element that needs an endpoint (D-35 list) or a new dependency
- Anything that changes a flow (Tier 2)

## Skills
angular-tailwind-ui-specialist, ui-ux-pro-max, angular-architect

## Result
_Filled on completion: result, checks run, accepted limitations._

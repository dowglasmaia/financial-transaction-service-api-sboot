# SPEC-FE-01 — UI/UX prototype and templates (designer agent)

**Status:** In progress (approved by the owner 2026-11-02) · **Depends on:** none · **Parallel with:** BE-01, BE-02
**Read first:** `../memory/produto.md`, `../memory/contexto-tecnico.md` (incl. *Frontend agent autonomy*) and the visual/flow reference `../../prototype/Maia Bank UI prototype/` (open `Maia Bank.dc.html`). Protocol: `../shared/como-executar.md` (tick a task only after verification + evidence line).

## Objective
Produce the clickable prototype, templates and tokens for every real flow plus the landing page and the AI Assistant concept.

## Scope
- Everything in `designer-brief.md` §1 (foundations, components, screens × breakpoints × themes, flows, handoff package)

**Out of scope:** Code; Features outside the brief; Native-app patterns; A functional AI Assistant

## Tasks
- [x] Send `designer-brief.md` to the designer agent
  > ✅ 2026-11-02 16:00 +0000 — prototype delivered and committed to the repo at `prototype/Maia Bank UI prototype/`. Verification: files present (`Maia Bank.dc.html`, `support.js`, `_ds/nocturne-*`). Not yet reviewed against brief §13.
- [ ] Review foundations + contrast report
  > ⏳ 2026-11-02 17:30 +0000 — Parcial: palette/typography/radius extracted from the HTML (see notes); contrast not measured. Falta: measured contrast report in both themes. Próxima ação: FE-02 measures it when the tokens are implemented, or the owner reviews visually.
- [ ] Review components and screens S-01..S-11, flows F-0..F-9 against brief §13
  > ⏳ 2026-11-02 17:30 +0000 — Parcial: static review done, checklist table above completed (pass/fail per item) and divergence list written. Falta: visual/interactive review by the owner in the originating design tool.
- [ ] Approve handoff package
  > ⛔ 2026-11-02 17:30 +0000 — Bloqueio: the package is incomplete (no tokens JSON, copy deck, accessibility annotations, component-states sheet). Para desbloquear: owner decides to request the missing artifacts or to accept extraction from the HTML in FE-02.

## Acceptance
- [ ] Brief §13 checklist fully ticked
- [ ] Human sign-off recorded

## Minimal validation (required to close this spec)
- [x] Checklist §13 completed with pass/fail per item
  > ✅ 2026-11-02 17:30 +0000 — table above (static review; caveats stated per item).
- [x] Divergence list written (seed for `docs/prototype-deviations.md`)
  > ✅ 2026-11-02 17:30 +0000 — 7 items under *Review notes*; FE-02 copies them into `docs/prototype-deviations.md` when the app folder exists.
- [ ] Written sign-off from the owner
  > ⛔ 2026-11-02 17:30 +0000 — Not executed: only the owner can sign off after reviewing the prototype visually.

## Stop and ask
- Design approval (human)

## Skills
ui-ux-pro-max, angular-tailwind-ui-specialist

## Checklist §13 results (static review by the agent — owner must confirm visually)
| # | Item (designer-brief §13) | Result | Evidence / note |
|---|---|---|---|
| 1 | Every [REAL] endpoint reachable via a flow; no screen without endpoint or [PROPOSAL] tag | **Partial** | Pix, Recharge, Withdraw, Statement, Detail map to real endpoints. **Deposit is a boleto flow** (client-side demo) that does not call `POST /deposit` as designed → Tier-2 proposal (divergence 1). Proposed/Coming-soon tags exist for boleto, presets, assistant. |
| 2 | All states (loading/empty/success/error + uncertain outcome) for S-02…S-09 | **Pass (logic)** | Prototype controls: data = Success/Loading/Empty/Error; outcome = Success/Business failure/Uncertain. Per-screen visual coverage not verified. |
| 3 | Dark and light pass contrast; both reviewed at 3 breakpoints | **Not verified** | `theme` (dark/light) and `bp` (desktop/tablet/mobile) states exist; no contrast measurement or visual check possible here. |
| 4 | No invented features; proposals visibly tagged | **Pass (with note)** | Presets, hide-balance, boleto, assistant are tagged "Proposed"/"Coming soon". |
| 5 | Copy matches §10, English only | **Mostly pass** | Strings inspected (e.g. "Current balance", "No activity yet", "Make a deposit to get started.", "Confirm and send", "The amount can't exceed your available balance.") match; a full line-by-line comparison of §10 was not done. |
| 6 | Components documented with all states; tokens exported | **Fail** | No tokens JSON, component-states sheet, copy-deck file or accessibility annotations in `prototype/` (brief §1.5, §12). |
| 7 | Browser experience only (no native-app patterns) | **Pass (static)** | No native patterns found; mobile uses a responsive "More" menu. |
| 8 | Landing/sign-in and AI Assistant concept exist, tagged Future/Proposed, no endpoint, no money movement | **Pass** | Screens `landing`, `signin`, `assistant`; assistant labelled "Coming soon". |
| 9 | "Demo environment · test data only" on every screen; polished modern online-bank feel | **Partial** | Chip present in the header/landing/footer text; polish and per-screen presence cannot be judged without rendering. |

**Visual review attempt:** a second attempt to open the prototype in the built-in browser was denied by the tool (local server origin not allowed); the first attempt rendered unfilled `{{ }}` templates. No interactive review has been possible in this environment.

## Review notes (draft by the agent, 2026-11-02 — static analysis; owner must confirm)
**How it was reviewed:** the prototype file (`Maia Bank.dc.html` + `support.js`) was read and its logic inspected. It does **not render outside its original design-canvas host** (opened from `file://` and from a local web server the `{{ }}` templates stay unrendered), so no interactive/visual review was possible here. **The owner must review it visually in the tool that produced it.**

**Coverage found in the prototype logic (vs `designer-brief.md`):**
- Screens: landing, sign-in (+ redirecting/error), Home, Statement, Detail, Pix (3 steps), Deposit, Recharge, Withdraw, result panel, system pages (not found, session expired, sign-out dialog), AI Assistant concept (chat, "Coming soon").
- Data states: Success / Loading / Empty / Error. Outcomes: Success / Business failure / Uncertain. Themes: dark/light. Breakpoints: desktop/tablet/mobile. "Demo environment · test data only" chip present. English copy.
- Pix follows the brief's flow incl. "Use full balance" (consistent with the decided `>=` rule); deposit-related and preset items are tagged "Proposed" inside the prototype.

**Divergences and gaps (seed for `docs/prototype-deviations.md`):**
1. **Deposit is modelled as an online boleto** (client-side "Simulate payment") but the backend is `POST /transactions/deposit {agency, account, amount}` → **Tier-2 proposal** needed (reconcile: boleto as a UI simulation whose "payment" calls the real deposit on the caller's own account from `/accounts/me`).
2. **Visual identity differs from brief §3** (warm dark-brown surfaces, gold `#CEA45C`, teal/green tones, Manrope + Sora, Phosphor icons vs Grafite Ciano/Inter). The prototype wins (recorded decision); **contrast is not verified** → measure in FE-02 and adjust (log it).
3. `_ds/nocturne-*` (blurple, Inter) is a bundled design-system export the main page does not appear to use → treat as non-authoritative.
4. Statement row→detail is tagged "Proposed" in the prototype; ids are **approved** (D-22) → remove the tag when implementing (BE-06 provides ids).
5. Sample data uses "Agency 0001 · Account 12345-6" (prototype only; real data comes from `/accounts/me`; the seed is 0211/2022).
6. **Handoff package incomplete:** no tokens JSON export, copy-deck file, accessibility annotations or component-states sheet found in `prototype/` (brief §1.5, §12). The frontend agent extracts tokens/copy from the HTML (FE-02) — decide whether to request the missing artifacts or accept this.
7. Not verifiable without a visual review: contrast, keyboard order/focus rings, reduced-motion variants, responsive behaviour per breakpoint, touch-target sizes.

**Sign-off:** pending the owner (see the unticked tasks).

## Notes
The frontend agent is **not limited to the prototype**: it may propose and apply improvements under the rules in `memory/contexto-tecnico.md` (*Frontend agent autonomy*). Review here should produce the initial list of known divergences (see that section).

## Result (pending sign-off)
Static review completed: checklist §13 with 9 items (3 pass, 3 partial/mostly, 1 not verifiable, 1 fail: incomplete handoff package, 1 pass-with-note) and a 7-item divergence list (biggest: deposit modelled as a boleto vs the real `POST /deposit`). **Not done:** visual/interactive review and contrast measurement (not possible here), owner sign-off. Accepted limitation proposal: FE-02 extracts tokens/copy from the HTML and measures contrast itself.

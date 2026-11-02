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
- [ ] Review components and screens S-01..S-11, flows F-0..F-9 against brief §13
- [ ] Approve handoff package

## Acceptance
- [ ] Brief §13 checklist fully ticked
- [ ] Human sign-off recorded

## Minimal validation (required to close this spec)
- [ ] Checklist §13 completed with pass/fail per item
- [ ] Divergence list written (seed for `docs/prototype-deviations.md`)
- [ ] Written sign-off from the owner

## Stop and ask
- Design approval (human)

## Skills
ui-ux-pro-max, angular-tailwind-ui-specialist

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

## Result
_Filled on completion: result, checks run, accepted limitations._

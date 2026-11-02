# Prototype deviations and proposals

The approved prototype (`prototype/Maia Bank UI prototype/`) is the starting point for look, flows and copy — not a limit.
This file logs every deviation. **Tier 1** = refinement inside the same flow (applied, logged here). **Tier 2** = changes a flow,
adds/removes a screen or capability, or needs the backend (written as a proposal, needs the owner's decision).

Seeded from the FE-01 review (`.spec/specs/fe-01-design-prototype.md`).

## Applied (Tier 1)
| # | Deviation | Why | Where |
|---|---|---|---|
| 1 | Dark `--line-input` `#6F5C4E` → `#75634F` | The prototype value gave 2.87:1 on raised surfaces; WCAG 2.2 AA needs 3:1 for control borders (now 3.17:1) | `src/styles.scss`, asserted by `tokens-contrast.spec.ts` |
| 2 | Added `--on-brand` / `--on-brand-muted` tokens (`#F5EEE2` / `#B3A28F`) | In the light theme the header is dark (`--brand` `#2B1A12`); text must not use the page content colour there (found during the visual check) | `src/styles.scss`, header, theme toggle |
| 3 | Tokens named semantically (`surface`, `surface-raised`, `content`, `accent`, `income`, `expense`…) instead of the prototype's short vars (`--s`, `--sr`, `--c`, `--a`…) | Readability, Tailwind utility names (`bg-surface`, `text-income`) | `src/styles.scss` |
| 4 | Icons via the `@phosphor-icons/web` package (regular + bold CSS) instead of loading from unpkg at runtime | No third-party script/CSS at runtime for icons; works offline in dev | `angular.json` styles |
| 5 | Fonts (Manrope, Sora) still come from Google Fonts like the prototype | Matches the design; self-hosting is a future privacy/perf improvement | `src/index.html` |
| 6 | Money rendered as `R$ 12,480.50` through one util (`formatBrl`) and never recomputed in the UI | Single formatting rule; backend is the authority for balances | `shared/util/money.ts` |
| 7 | Ledger row shows direction by rail + icon + label + sign + screen-reader text | Colour must not be the only cue (a11y) | `shared/ui/ledger-row.component.ts` |

## Proposals / open (Tier 2 — need the owner)
| # | Proposal | Detail |
|---|---|---|
| A | **Deposit flow** | The prototype models deposit as an online *boleto* (client-side demo with "Simulate payment"). The backend is `POST /transactions/deposit {agency, account, amount}`. Suggested reconciliation: keep the boleto screen as a UI simulation; "Simulate payment" calls the real deposit for the caller's own account (from `GET /accounts/me`). Decide in FE-04. |
| B | **Sample data in the prototype** | "Agency 0001 · Account 12345-6" is placeholder data; real values come from `/accounts/me`. No action unless the owner wants it mirrored. |
| C | **`_ds/nocturne-*` bundle** | A bundled design-system export (blurple, Inter) that the main prototype page does not use. Treated as non-authoritative and not imported. |
| D | **Handoff artifacts** | The prototype has no tokens JSON, copy deck or a11y annotations; tokens/copy were extracted from the HTML. Request them only if the owner wants a formal handoff. |

## Decisions recorded in FE-02
- **Test runner:** Vitest via Angular's `@angular/build:unit-test` (`npm test`), jsdom. Reason: Angular 21's default runner, no Karma/Jasmine stack.
- **Chart library:** none. The prototype's charts are hand-made SVG/CSS; Home (FE-03) builds them the same way (zero dependency, full control of the accessible table fallback).
- **New dependencies:** `@phosphor-icons/web` (icons, design-mandated), `vitest`, `jsdom`, `@types/node` (dev). No UI/state library.

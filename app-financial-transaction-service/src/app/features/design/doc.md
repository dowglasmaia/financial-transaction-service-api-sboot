# Design (living style guide)

## Purpose
The `/design` route shows every design token and shared component in all its states, in both themes (toggle in the
header). It is the review surface for the visual identity extracted from the prototype and a regression aid for
accessibility checks (axe) and contrast.

## Journeys
Open `/design`, toggle dark/light, tab through the controls (focus ring), review each section.

## Contract
No API operations. Static sample data only (clearly labelled as examples).

## State
No shared state. Local signals for the demo controls (chip selection, loading buttons, money input).

## Validation
Contrast of every token pair, in both themes, is asserted by `core/theme/tokens-contrast.spec.ts`.

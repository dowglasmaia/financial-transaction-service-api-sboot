# Deposit

## Responsibility
Deposit money into the signed-in customer's OWN account, with the v2 experience: enter an amount → **Generate boleto** → **Simulate payment**. The boleto is a **demo** built on the device (`domain/boleto.ts`, deterministic from the amount); nothing is issued, stored or fetched. Every boleto surface says it is a demo ("Maia Bank (demo)", "Awaiting payment (demo)", "Demo. This is not a real boleto.").

## Contract
`POST /transactions/deposit {agency, account, amount}` with the optional `Idempotency-Key` header. Agency and number come from `/accounts/me` (the customer's own account). 204 = registered; 404 "Account not found." = field error on the amount; 422 = business panel; no answer / 5xx = uncertain panel.
Third-party deposits remain allowed by the API (rule 7) but are not offered here.

## Pieces
* `data-access/deposit.api.ts` — the only HTTP code.
* `domain/boleto.ts` — `buildBoleto(amount, payer, today)`, pure and deterministic.
* `pages/deposit-page` — uses the shared `OperationController`, `app-amount-presets` (no balance limit, presets 100/250/500/1000), `app-result-panel`. "Download boleto", "Share" and "Copy line" only show the toast "Demo action". The balance is re-read from the API after success.

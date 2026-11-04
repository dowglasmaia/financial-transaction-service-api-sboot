# Recharge

## Responsibility
Top up a mobile phone from the signed-in customer's account: carrier, masked number, amount with proposed values, review, one request, the three results. The debit and the ledger row are immediate; the carrier credit is **asynchronous** (a Kafka event through the transactional outbox), so every success copy says the recharge was **requested**, never "completed": *"Recharge requested. Your carrier will apply the credit shortly."*

## Contract
`POST /transactions/recharge-mobile {areaCode, phoneNumber, carrier, amount}` with the optional `Idempotency-Key` header. Carriers are the backend enum `VIVO | CLARO | TIM | ALGAR` (v2's "Oi" became Algar). The masked number `(11) 9 1234-5678` is split into `areaCode` (11) and `phoneNumber` (912345678) by `shared/util/phone.ts` (same rule as the backend, `^[1-9]{2}9[0-9]{8}$`; feedback only, the server decides). 422 shows the backend message (for example "Invalid cell phone numer"); no answer / 5xx is the uncertain panel.

## Pieces
* `data-access/recharge.api.ts` — the only HTTP code.
* `shared/ui/recharge-form.component.ts` — carrier cards (text wordmarks, no logos), masked number, amount presets 15/30/50/100; shared by this page and the Home quick-recharge card.
* `pages/recharge-page` — shared `OperationController`, `app-review-dialog` (carrier / number / amount), `app-result-panel`. When it is opened from Home it reads the form from `history.state.recharge` and opens the same review (nothing is sent from Home).

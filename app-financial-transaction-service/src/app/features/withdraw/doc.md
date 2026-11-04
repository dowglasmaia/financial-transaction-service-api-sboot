# Withdraw

## Responsibility
Withdraw money from the signed-in customer's own account: amount with proposed values, review, one request, and the three possible results. The body is `{amount}` only; the server decides everything (balance rule, ownership).

## Contract
`POST /transactions/atm-withdrawal` with the optional header `Idempotency-Key` (contract 1.2.0). 204 = registered; 422 = refused (the message is shown); no answer / 5xx = outcome unknown.

## Shared pieces built here (reused by Deposit, Recharge and Pix)
* `shared/operation/operation.ts` — **`OperationController`**, the operation shell. `run(send)` starts a user attempt with a NEW `Idempotency-Key`; while a request is in flight further `run` calls are ignored (double click = one request); there is **no automatic retry**; `retry()` (only after an uncertain outcome) repeats the same request with the SAME key; `reset()` returns to the form. Outcome: 2xx → `success`, 4xx → `business`, no answer / timeout / 5xx → `uncertain`.
* `shared/ui/amount-presets.component.ts` — `app-amount-presets`: proposed amounts, "Use full balance", the money input, the balance line and early feedback (`amountFeedback`).
* `shared/ui/review-dialog.component.ts` — `app-review-dialog`: receipt lines, Back / Confirm; inert and not dismissible while pending.
* `shared/ui/result-panel.component.ts` — `app-result-panel` (`success` | `business` | `uncertain`) with its actions.

Usage in a page: keep an `OperationController`, open `app-review-dialog` on Review, call `operation.run((key) => api.xxx(body, key))` on `confirmed`, show `app-result-panel` once `phase()` is not `idle`/`pending`, refresh the balance from the API on success, wire `(retry)` to `operation.retry()` and `(edit)` to `operation.reset()`.

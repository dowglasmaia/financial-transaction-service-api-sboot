# Pix

## Responsibility
Send a Pix by key from the signed-in customer's account, in three steps (**Key → Amount → Review**) and then one of three results. The client rules only give early feedback; the server decides everything.

## Contract
`POST /transactions/send-pix {pixKey, keyType, transactionAmount}` with the optional `Idempotency-Key` header. `keyType` is `cpf | email | phone`. 204 = sent; 422 = refused (bad key, balance, "Fail send Pix."), balance unchanged; no answer / 5xx = outcome unknown. Behind it: reserve (debit + `PENDING`) → MaiaBank → confirm or compensate. No local recipient is credited.

## Rules (mirrored from the backend, same case lists in `shared/util/pix-key.spec.ts` and `PixKeyRulesTest`)
* CPF: 11 digits, plain or `999.999.999-99`, not all the same digit, both check digits right. The request carries the digits only.
* E-mail: at most 77 characters, letters/digits separated by single `. _ % + -`, domain labels without edge hyphens, letters-only TLD of 2 to 24.
* Mobile: `^[1-9]{2}9[0-9]{8}$` over the digits; shown as `(11) 9 1234-5678`.

## Privacy
The key lives only in the page component. The review and the receipt show it **masked** (`•••.982.•••-25`, `(11) 9 ••••-5678`, `m•••@example.com`); it is never logged, never in a URL, and is cleared when a new Pix starts. The server logs it masked too (`PixKey.masked()`).

## Pieces
* `data-access/pix.api.ts` — the only HTTP code.
* `shared/util/pix-key.ts` — rules, masks, feedback.
* `pages/pix-page` — stepper, segmented key type, `app-amount-presets` (50/100/200/500, full balance, **Continue is blocked above the balance**), the shared `OperationController` and `app-result-panel`. Business failure says "We couldn't send this Pix. Your balance was not changed." with the backend's reason; uncertain has the explicit **Try again** (same key).

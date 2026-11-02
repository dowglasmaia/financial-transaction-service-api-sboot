# Product memory — what we are building and what must not break

> Read this first, then `contexto-tecnico.md`, then the one spec you are executing.

## What it is
A small **online-bank web app (browser only)** with a Spring Boot API. It is a **functional portfolio prototype**: real endpoints, test data, modern neobank look. Not a production bank. Single user persona: the account holder. One account per user. UI language: **English**. Docs: English.

## What exists today (the only real features)
| Capability | Endpoint (`/api/v1`) | Request | OK | Errors (message → status) |
|---|---|---|---|---|
| Send Pix | `POST /transactions/send-pix` | `pixKey`, `keyType` (`cpf`/`email`/`phone`), `transactionAmount` | 204 | invalid/missing field, bad key, no balance, "Fail send Pix." → 422 |
| Deposit | `POST /transactions/deposit` | `agency`, `account`, `amount` | 204 | "Account not found." → 404; invalid → 422 |
| Mobile recharge | `POST /transactions/recharge-mobile` | `areaCode`, `phoneNumber`, `carrier` (VIVO/CLARO/TIM/ALGAR), `amount` | 204 | "Invalid cell phone numer", balance → 422 |
| ATM withdrawal | `POST /transactions/atm-withdrawal` | `amount` (+ optional `agency`,`numberAccount`) | 204 | 404 account, 422 balance |
| Statement | `GET /transactions?initial-date&final-date` (`dd/MM/yyyy`) | — | 200 `{transaction[{transactionType,amount,dateHour}], sumIncoming, sumOutgoing, balance}` | — |
| Transaction | `GET /transactions/{id}` | — | 200 `{transactionType,amount,dateHour}` | 404 |
Error body is always `{code, message}` (`code` = HTTP status name, e.g. `UNPROCESSABLE_ENTITY`). Success writes return **204 with no body**.
Ledger types: `PIX`, `PHONE_RECHARGE`, `WITHDRAW` (outgoing), `DEPOSIT` (incoming).

## Rules that must be preserved (regression contract)
1. 204/404/422 statuses, `{code,message}` shape and existing messages stay.
2. `amount > 0` on every operation.
3. Pix key formats: CPF (check digits), e-mail regex, phone `^[1-9]{2}9[0-9]{8}$` (area code + 9 digits). Key "existence" is not verified (stub) — keep it that way.
4. Recharge phone = `areaCode + phoneNumber` matching the same phone regex; carrier must be one of four.
5. Pix = reserve (debit + PENDING ledger row) → call MaiaBank (`POST` to `api.client.maiabank.url`) → confirm (COMPLETED) or compensate (credit back, FAILED). On failure the customer is **never left debited**. **No local recipient is credited** (decided). The statement only includes COMPLETED rows.
6. Recharge = debit + ledger + event written in ONE transaction; the original request JSON reaches Kafka topic `recharge-mobile-topic` through a transactional outbox (same payload bytes, at-least-once). No consumer exists yet.
7. Deposit credits **any** existing account number (third-party deposits allowed); the typed agency is ignored by the backend (uses agency `0211`).
8. Statement: `sumIncoming` = Σ DEPOSIT; `sumOutgoing` = Σ PIX+PHONE_RECHARGE+WITHDRAW; `balance` = current balance of the caller's account.
9. Ledger `dateHour` = server time at insert.

## Decided changes (these are the only intended behaviour changes)
| Decision | Change |
|---|---|
| Balance rule | Debit allowed when `balance >= amount` — **implemented in BE-03** (atomic, single place). |
| Ownership | A user may only debit accounts they own; ledger row belongs to the **debited** account; statement and transaction lookup limited to the caller's account (today unscoped = data leak). |
| Statement dates | Fix so a single-day range returns that day (today the initial day is excluded; dates are end-of-day UTC). Time zone for day boundaries: to be chosen in spec BE-05. |
| Auth | Auth0 (OIDC, Authorization Code + PKCE, SPA app); API is a JWT resource server with a role claim (`auth-auth0.md`). |
| Additive API | `GET /accounts/me` → `{agency, number, balance}`; `id` on each statement row; pagination/sort/type filter; optional `Idempotency-Key`. |
| Schema | Flyway, clean `V1` (database is disposable). |

## Do NOT invent (does not exist, not planned)
Account registration, categories, budgets, cards/invoices, recurring or scheduled payments, internal transfers, reversals/cancellations, multi-account per user, export, notifications, native/mobile app, offline mode, real KYC.
**AI Assistant = future plan**: only a non-functional "Coming soon" design concept (`designer-brief.md` S-11). No backend work.

## Frontend scope (Angular 21, Tailwind, dark default + light; visual reference = `prototype/`)
Screens: landing/sign-in, Home (balance, period totals, chart, recent), Statement (+ detail), Pix, Deposit, Recharge, Withdraw, result panel, error pages, AI Assistant concept. Visual and flow reference: the approved prototype in `prototype/`; original requirements and copy deck: `designer-brief.md`. The frontend agent may propose improvements (see `contexto-tecnico.md`, *Frontend: prototype and agent autonomy*). Rules the UI must follow: backend is the authority (never compute balance), no auto-retry of POSTs, "Recharge requested" (never "completed"), after a network error on a write tell the user to check the statement, money shown as `R$ 1,234.56`.

## Pending (only these)
MaiaBank request body contract (only if Pix payload must change) · day-boundary time zone · rounding for >2 decimals · whether generated `javax` validations are enforced (answered by spec BE-06 evidence).

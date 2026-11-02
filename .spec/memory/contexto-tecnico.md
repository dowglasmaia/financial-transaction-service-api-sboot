# Technical memory — how the system is built and how to change it safely

> Pair with `produto.md`. Evidence tags: **[CODE]** read in source · **[INFERRED]** not executed. Nothing here was compiled or run (no JDK on the analysis machine).

## Stack
Current: Java 17, Spring Boot 3.2.5, Spring Cloud 2023.0.1, Maven wrapper, PostgreSQL 14, Spring Data JPA/Hibernate, Kafka (producer only), OpenFeign + Resilience4j (`MaiaBankClient`), Actuator/Prometheus, Lombok 1.18.24, swagger-codegen 3.0.36 (generates `com.dowglasmaia.provider.{api,model}` DTOs/interfaces from `src/main/resources/swagger/financial-transaction-openapi.yaml`), Docker + docker-compose (api 8088, pg-db, kafka, zookeeper, kafdrop, maiabank-api 8089).
Target: **Spring Boot 3.5.x → Java 21 → Java 25** (no Boot 4), openapi-generator (jakarta), Flyway, Spring Security resource server. CI: GitHub Actions. Runtime: Docker.

## Repository layout (monorepo)
```
api-financial-transaction-service/   Spring Boot API (Maven)      CI: .github/workflows/api-ci.yml
app-financial-transaction-service/   Angular web app (created by FE-02)   CI: .github/workflows/app-ci.yml
prototype/Maia Bank UI prototype/    approved UI/UX prototype (visual + flow reference)
.spec/                               memory, specs, execution flow
docker-compose.yml                   local stack (builds the API from its folder)
```
Each app has an isolated, path-filtered pipeline. Run backend commands from `api-financial-transaction-service/`, frontend commands from `app-financial-transaction-service/`.

## Code map (package root `com.dowglasmaia.financialtransaction`, under `api-financial-transaction-service/src/main/java/`) — **do not move or rename packages**
`controllers` (`TransactionController` implements generated `TransactionsApi`; `validate.ValidateBodyRequest`) · `services` (+`impl`: Pix, Deposit, RechargeMobile, AtmWithdrawal, Transaction; `impl.mapper.TransactionMapper`) · `services.client.account|agency|maiaBank` (local helpers + Feign client) · `entity` (User→table `usuario`, Agency, Account, Transaction, TransactionTypeEnum) · `repository` · `config` (`HandlerException`, `kafka`, `db_massa_test`) · `exeptions` (sic) · `utils` (CPF/e-mail/phone validators, `DateConverter`).
Schema is owned by **Flyway** (`db/migration/V1__baseline.sql`; Hibernate `ddl-auto: validate`; `transaction.account_id` has no FK until BE-03 fixes F-08). Data: `usuario(id,name,cpf uniq,email uniq,phone_number,area_code)`, `agency(id,number)`, `account(id,agency_id,user_id,number,balance)`, `transaction(id,date_hour,amount,account_id,agency_id,transaction_type)` — UUID strings as ids, FK only on `transaction.agency_id`, index `(account_id, date_hour desc)`.

## Hard constraints
* **Package structure and architecture stay** (controller → service → repository). Improve inside existing packages; new classes go into existing packages.
* Behaviour contract in `produto.md` is frozen except the listed decided changes.
* Money is `BigDecimal` (scale 2). No float math.
* Every behaviour change needs a test first (characterization → flip).
* No secrets in tracked files; test data never real (replace seed CPF/e-mail).
* Contract changes (OpenAPI) go through the `openapi-rest-contract` skill: **preview → explicit approval → generate**.

## Known defects (what the specs fix)
| ID | Defect [CODE] | Fixed in |
|---|---|---|
| F-01 | No auth; identity = startup singleton from test seed (`DBService`) | BE-04 |
| F-02 | No `@Transactional` anywhere; balance/ledger/remote/Kafka commit separately; deposit swallows ledger failure | BE-03 |
| F-03 | Read-modify-write on `balance`, no lock/`@Version`; Pix "restore balance" overwrites concurrent changes | BE-03 |
| F-04 | Statement + sums unscoped (all accounts); `GET /transactions/{id}` no ownership | BE-04 |
| F-05 | Non-card withdraw debits any account by number, ledger on caller | BE-04 |
| F-06 | ~~Default profile `test` + `ddl-auto: create` wipes DB; DB credentials in tracked `application.yaml`~~ **fixed in BE-02** (old dev credential `maia/maiapw` stays in git history — treat as burned, rotate where reused) | BE-02 |
| F-07 | Both statement dates → end-of-day UTC (initial day excluded); null date → NPE | BE-05 |
| F-08 | Recharge ledger stores account **number** in `accountId` (others store id); Kafka publish is a dual write | BE-03 |
| F-09 | Pix: `receiversPix()` is an empty POST; key-existence stub; `agencyId` null | BE-03 |
| F-10 | `AddAmountService.addAmountToAccount` actually subtracts; balance rule duplicated 3× | BE-03 |
| F-11 | Validators: `nonNull(b) && x==null \|\| x.isEmpty()` → NPE; CPF validator lacks length check; generated bean validation likely inert (javax; proven in BE-06) | BE-05 |
| F-12 | Handler ignores `HttpStatus` in exceptions (uses `FOUND`); no generic/parse-error handler; `@EnableWebMvc` on advice; OpenAPI says 200/400/401, real 204/404/422 | BE-05 (handler), BE-06 (contract) |
| F-14 | Lombok 1.18.24, compiler plugin 3.8.1, Sleuth BOM, protobuf 3.18.1, javax APIs, Boot 3.2.5 block Java 25 | BE-07 |
| F-15/17/18 | OSIV on, EAGER `@ManyToOne`, no indexes/pagination; PII in logs; unpinned Docker tags, no CI | BE-01 (CI), BE-02 (indexes, tags), BE-06 (pagination), BE-08 (logs) |
| F-19 | Dead code (`TransactionRequestDTO`, `AccountModel`, MapStruct), typos | BE-08 |
| F-20 | `@CircuitBreaker` on Feign interface unverified; slow-call threshold 100 ms; no Feign timeouts | BE-03 |
Tests today: 3 classes, weak (`AtmWithdrawalServiceImplTest` asserts nothing); no CI.

## Target design in one screen
* Each money operation = one `@Transactional` service method; debit via atomic conditional update (`WHERE balance >= :amt`) or `@Version`; ledger written in the same transaction.
* Pix: reserve (debit + ledger PENDING) → commit → call MaiaBank → confirm/compensate; Kafka via transactional outbox.
* `CurrentUser` bean (package `services.client`) resolves Okta `sub` → `usuario.external_subject` → the single account. Unknown `sub` → 403. Scopes `transactions:read|write`, role `customer`.
* Statement: account-scoped, half-open date range, paginated, totals by aggregate query, rows carry `id`.
* `GET /accounts/me`. Idempotency-Key on POSTs (store in DB).

## Decisions log (final)
Okta Integrator Free plan (10 users; limits in `auth-okta.md`) · Boot 3.5.x via Java 21 then 25 · keep architecture/packages · `>=` balance rule · Pix behaviour kept (no local credit) · owner-only debits · English UI · visual identity = the approved prototype in `prototype/` (supersedes the earlier "Grafite Ciano" proposal; skills' stack assumptions ignored) · additive API (`/accounts/me`, row `id`) · manual user↔Okta linking, role `customer` only · disposable DB, clean Flyway V1 · no Kafka consumer, payload frozen · one account per user · GitHub Actions + Docker · web-only portfolio prototype · AI Assistant = future concept only.

## Frontend: prototype and agent autonomy
**Reference:** `prototype/Maia Bank UI prototype/Maia Bank.dc.html` (design-canvas export with `support.js`; **do not copy its markup** — reimplement in Angular). Observed identity [INFERRED from the file, confirm when opening it]: warm dark-brown surfaces, gold accent (`#CEA45C`), teal/green status tones, Manrope + Sora fonts, Phosphor icons, "Demo environment · test data only" chip, prototype controls (data state, on-confirm outcome, screen jump). `_ds/nocturne-*` inside it is a bundled design-system export (blurple, Inter) that the main page does not appear to use — treat as non-authoritative.
Screens in the prototype: landing/sign-in, Home, Statement (+detail), Pix (3 steps), Deposit, Recharge, Withdrawal, result panel, AI Assistant concept (Coming soon).

**The frontend agent is not limited to the prototype.** It is the starting point for look, flows and copy, not a cage. The agent has autonomy to propose and apply improvements to UX details, accessibility, responsiveness, performance, component structure, microcopy, motion, form ergonomics, empty/error states and edge cases the prototype missed.
* **Tier 1 — apply directly and log:** refinements inside the same flow and scope (spacing, states, validation UX, a11y, motion, copy, component design). Log each in `app-financial-transaction-service/docs/prototype-deviations.md` (what / why / screens).
* **Tier 2 — propose and stop:** anything that changes a flow, adds/removes a screen, adds a capability, or needs backend work. Write a short proposal in the same file and ask.
* **Never (guardrails):** break the backend contract or `produto.md` rules; invent capabilities without an endpoint (propose instead); auto-retry POSTs; ship non-English copy; add native-app patterns.
* **Where prototype and backend disagree, the backend contract wins** and the agent proposes the reconciliation. Known case: the prototype's **Deposit is an online boleto flow** ("Proposed · client-side demo" with "Simulate payment"), while the real endpoint is `POST /transactions/deposit {agency, account, amount}` — a Tier-2 proposal is needed (e.g. boleto step is a UI simulation whose "payment" calls the real deposit for the caller's own account from `/accounts/me`).
* Prototype palette/typography replace the earlier Grafite Ciano tokens as the starting point; the agent may refine them (contrast ≥ 4.5:1 text, ≥ 3:1 UI) and logs changes.

## How the agent must work
1. Read `produto.md`, this file, the spec you were given, and follow `../spec-execution-flow.md` for the execution loop. Read `auth-okta.md` / `designer-brief.md` only for auth or UI work.
2. Work **only** within the spec scope; list extra ideas as proposals.
3. Tests first for behaviour changes; run the suite; record an evidence line per task (`shared/como-executar.md`).
4. Stop and ask at the spec's approval gates.
5. Skills: `java-modern-engineering` (Java/Spring code, Mode A: keep existing architecture) · `java-persistence-performance` (SQL, JPA, transactions, Flyway) · `openapi-rest-contract` (contract) · `angular-frontend-scaffold` (once, project creation) · `angular-module-scaffold` (feature modules) · `angular-tailwind-ui-specialist` (UI) · `angular-architect` (Angular design) · `ui-ux-pro-max` (UX review). Skills mention a different project ("Finance Controller": Boot 4.1.1, Modulith, `owner_id`) — ignore those stack assumptions.

## Run / verify (expected, unverified here)
`cd api-financial-transaction-service && ./mvnw -B verify` · `cp .env.example .env` then `docker compose up --build` from the repo root (API on 8088; DB credentials come from `.env`, never from tracked files) · Postman collection in `src/main/resources/postman_collection/`.

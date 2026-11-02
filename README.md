# Financial Transaction Service (monorepo)

Functional portfolio prototype of a modern online bank (web only, test data).

| Folder | What | CI |
|---|---|---|
| `api-financial-transaction-service/` | Spring Boot API (Pix, deposit, recharge, withdrawal, statement) | `.github/workflows/api-ci.yml` |
| `app-financial-transaction-service/` | Angular web app (created by spec FE-02) | `.github/workflows/app-ci.yml` |
| `prototype/` | Approved UI/UX prototype - visual and flow reference for the frontend | - |
| `.spec/` | SDD knowledge base: `memory/`, `specs/`, execution flow | - |
| `docker-compose.yml` | Local stack (API, PostgreSQL, Kafka, MaiaBank stub) | - |

Each application has its own isolated pipeline (path-filtered) and is built and tested independently.

Run the stack: `docker compose up --build` (API on `localhost:8088`). API details: `api-financial-transaction-service/README.md`. Work is driven by `.spec/spec-execution-flow.md`.

# `.spec/` — how to use it

Spec-Driven Development for a **small web banking prototype** (monorepo: `api-financial-transaction-service` + `app-financial-transaction-service`; UI reference in `../prototype/`). Keep reading short:

| Order | File | Use |
|---|---|---|
| 1 | `memory/produto.md` | What the product does and the rules that must not break |
| 2 | `memory/contexto-tecnico.md` | Stack, code map, constraints, known defects, decisions, how to work |
| 3 | `spec-execution-flow.md` | **How and in what order to execute every spec** (loop, master order, per-spec runbooks) |
| 4 | `specs/README.md` → one spec | The task to execute (order, dependencies, acceptance) |
| as needed | `auth-auth0.md` | Auth work (Auth0, OIDC, Spring Security, Angular) |
| as needed | `designer-brief.md` | Standalone brief for the UI/UX designer agent |
| prompts | `prompts.md` | Ready-to-paste prompt per spec (with minimal validation) |
| protocol | `shared/como-executar.md` | How to execute and record evidence |
| template | `template/SPEC_TEMPLATE.md` | Shape of a spec |

Current state: foundation specs (MONO-01, BE-01…BE-04, FE-01, FE-02) are done; the rest is delivered as **vertical slices SL-00…SL-06** (backend + frontend + e2e per branch/PR, D-31) followed by BE-07, BE-08, FE-05 and FINAL-01. Live status per spec: `specs/README.md`. Visual reference: `prototype/Maia Bank UI prototype/Maia Bank v2.dc.html`.

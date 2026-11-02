# `.spec/` — how to use it

Spec-Driven Development for a **small web banking prototype** (monorepo: `api-financial-transaction-service` + `app-financial-transaction-service`; UI reference in `../prototype/`). Keep reading short:

| Order | File | Use |
|---|---|---|
| 1 | `memory/produto.md` | What the product does and the rules that must not break |
| 2 | `memory/contexto-tecnico.md` | Stack, code map, constraints, known defects, decisions, how to work |
| 3 | `spec-execution-flow.md` | **How and in what order to execute every spec** (loop, master order, per-spec runbooks) |
| 4 | `specs/README.md` → one spec | The task to execute (order, dependencies, acceptance) |
| as needed | `auth-auth0.md` | Auth work (Auth0, OIDC, Spring Security, Angular); config/verification via the project skill `.claude/skills/auth-oidc-config/` |
| as needed | `designer-brief.md` | Standalone brief for the UI/UX designer agent |
| prompts | `prompts.md` | Ready-to-paste prompt per spec (with minimal validation) |
| protocol | `shared/como-executar.md` | How to execute and record evidence |
| template | `template/SPEC_TEMPLATE.md` | Shape of a spec |

Nothing has been implemented; all specs are Draft. No source, DB or build file has been changed by this analysis.

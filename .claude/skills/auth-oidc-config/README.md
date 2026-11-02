# auth-oidc-config

Project skill (auto-discovered by Claude Code in this repository). Hybrid: deterministic Node scripts (no dependencies, Node 18+) plus a short decision layer in `SKILL.md`.

```bash
node scripts/check-inputs.mjs examples/financial-transaction.inputs.json
node scripts/generate.mjs examples/financial-transaction.inputs.json --out ./auth-config-out
node scripts/verify-tenant.mjs examples/financial-transaction.inputs.json   # public endpoints only
node --test scripts/test/auth.test.mjs
```

To use it from any project, copy this folder to `~/.claude/skills/auth-oidc-config/`.

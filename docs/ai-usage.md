# How AI tools were used

AI was used as a collaborator across the lifecycle, with every output reviewed, tested and owned by me. This log records **what** I used AI for, **how** I steered it, and **where it was wrong** and how that was caught.

## Tools

| Tool | Used for |
|------|----------|
| Claude (Claude Code / chat) | Planning, requirements drafting, architecture options, code generation under TDD, test design, review |
| _add others as used (Copilot, Cursor…)_ | |

## Guardrails

- `CLAUDE.md` at the repo root gives the agent explicit rules: test-first, money as `BigDecimal`, package-by-feature, no scope creep.
- Every AI-generated change goes through the test suite and my review before it is committed.
- Domain decisions (scope, currency handling, what to leave out) were made by me; AI was used to challenge and articulate them.

## Log

### 1. Planning & requirements

- **Prompt (summary):** shared the job description and assessment brief; asked for an implementation plan covering stack, scope, data model, testing and commit strategy.
- **Kept:** package-by-feature structure, SQL-side aggregations, deterministic seeding, documenting exclusions explicitly.
- **Changed / decided myself:** _fill in — e.g. chose PostgreSQL over the SQLite suggested by the brief, trimmed features, etc._

### Corrections (where AI was wrong)

| Date | What the AI produced | How it was caught | Fix |
|------|---------------------|-------------------|-----|
| 2026-10-15 | Plan specified Spring Boot 3, which had left open-source support (June 2026) by scaffolding time | Spring Initializr no longer offered any 3.x version | Moved to Spring Boot 4.1 — [ADR 006](adr/006-spring-boot-4.md) |
| 2026-10-15 | Plan specified PrimeNG as a free UI library; it is now under a commercial licence requiring a key | `npm install` pulled in `@primeui/license-manager`; read the package's LICENSE.md | Switched to Angular Material — [ADR 007](adr/007-angular-material-over-primeng.md) |

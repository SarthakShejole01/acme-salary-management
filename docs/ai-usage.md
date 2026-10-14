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
| _to be filled as it happens_ | | | |

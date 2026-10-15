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
| 2026-10-15 | Frontend tests kept passing after the template they checked was changed (Vitest 5 on-disk module cache) | Deliberately broke the toolbar text; tests stayed green | Disabled `fsModuleCache` in `vitest.config.ts`; re-verified a template change turns tests red |
| 2026-10-15 | Persistence round-trip test read the entity back from the JPA persistence context, not from PostgreSQL | Code review of the test before adding timestamp checks | Clear the `TestEntityManager` before reading back |
| 2026-10-15 | Shared test helper for insights created a new instance per call, restarting its counter (duplicate employee codes) | Reviewed the helper while wiring it in, before committing | One helper instance per test |
| 2026-10-15 | Histogram width of 2.5 × 10⁻² = 0.025 — three decimals, which `Money` rejects | Self-review of `HistogramBins` | Test reproducing it, then round sub-cent widths up to whole cents |
| 2026-10-15 | CSV formula-injection test asserted the wrong shape for values that also need quoting | Re-read the test against RFC 4180 before implementing | Corrected the assertion (unpushed commit amended) |
| 2026-10-15 | CSV exporter test omitted the JPA auditing config (`created_at` null); a `&&` chain committed the red feature commit because a later `grep` succeeded | Test run output showed 5 failures right after the commit | Fixed the test setup and rebuilt the two unpushed commits; check results before committing |
| 2026-10-15 | Shell heredocs silently halved backslashes / turned `
` into real line breaks in Java sources | Compiler errors | Write such files with the editor tool or a script file instead of inline shell strings |
| 2026-10-15 | One commit used a `perf:` prefix, not in the CLAUDE.md commit types | Self-review when reporting | Left as pushed; use `chore:`/`feat:` going forward |
| 2026-10-15 | Employees page search box used a `[value]` binding, so Material's form field kept stale state after "Clear filters"; a failed load still showed the previous employee count | Manual browser check (not caught by the first unit tests) | Tests reproducing both, then a `FormControl` and a count gated on the loaded state |
| 2026-10-15 | Salary hint text in the add-employee dialog overlapped the next field's label | Manual browser check | Shorter hint |
| 2026-10-15 | SVG chart centred the first and last axis labels on the plot edges (clipped) and printed every label even when they overlapped ("SGD 20,000" ×10) | Measured label bounding boxes for all eight countries in the browser; screenshots alone were stale and showed nothing wrong | Tests first, then padded plot and thinned labels; re-measured: 0 overlaps in all eight |
| 2026-10-15 | Employees page swallowed a failed reference-data request (`catchError` → empty lists), so Add opened with no countries and no message; tables unnamed; layouts overflowed at 400 px | Deliberate UI audit: blocked the endpoint in the live app, ran a DOM accessibility check, narrowed the window and measured overflow | Tests first for the state and table names; then CSS wrap/scroll fixes verified by measurement |

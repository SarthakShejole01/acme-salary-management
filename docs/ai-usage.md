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
- **Changed / decided myself:** chose PostgreSQL over the SQLite suggested by the brief ([ADR 002](adr/002-postgresql-over-sqlite.md)); kept the scope to base salary and wrote the exclusions down ([requirements](requirements.md)); chose local currency with fixed, dated rates ([ADR 003](adr/003-local-currency-with-fixed-exchange-rates.md)). During the build, when the plan met reality: moved to Spring Boot 4.1 because Boot 3 is out of open-source support ([ADR 006](adr/006-spring-boot-4.md)); picked Angular Material over a commercially licensed PrimeNG ([ADR 007](adr/007-angular-material-over-primeng.md)); upgraded Node to run Angular 22 rather than step down a major version; set the branch-per-change, pull-request-into-`dev` workflow; merged each pull request myself.

### What the corrections have in common

1. **Stale knowledge about a moving ecosystem.** The plan named Spring Boot 3 and PrimeNG; both had changed by the day they were used. The cheap defence was to check the tool (Spring Initializr, `npm view`, the package licence) before building on a version.
2. **Tests that passed for the wrong reason.** A cached template, a read from the persistence context, a mock that does not follow a forward, a count assertion that held while the bug was live. The defence was to **break the code on purpose** and watch the test fail, and to commit every test red before its implementation.
3. **Bugs only a real run exposes.** Clipped and overlapping chart labels, an Add dialog with no countries after a failed request, a table overflowing at 400 px: unit tests were green for all of them. They were found by driving the running application in a browser, measuring the page, and blocking an endpoint on purpose.
4. **Tooling slips, not logic errors.** Shell quoting that altered Java source, a command chain that committed after a failure. The defence was reading `git show --stat` and the compiler output instead of trusting a silent success.

Apart from one commit prefix noticed after merging, each row was caught before the change was merged: by a test, a build, a review of the diff or a manual check, which is the point of the working agreement in [`CLAUDE.md`](../CLAUDE.md). The two outdated-ecosystem assumptions sit in the original plan documents and were corrected when the backend and frontend were scaffolded.

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
| 2026-10-15 | Shell heredocs silently halved backslashes / turned backslash escapes such as `\r\n` into real line breaks in Java sources | Compiler errors | Write such files with the editor tool or a script file instead of inline shell strings |
| 2026-10-15 | One commit used a `perf:` prefix, not in the CLAUDE.md commit types | Self-review when reporting | Left as pushed; use `chore:`/`feat:` going forward |
| 2026-10-15 | Employees page search box used a `[value]` binding, so Material's form field kept stale state after "Clear filters"; a failed load still showed the previous employee count | Manual browser check (not caught by the first unit tests) | Tests reproducing both, then a `FormControl` and a count gated on the loaded state |
| 2026-10-15 | Salary hint text in the add-employee dialog overlapped the next field's label | Manual browser check | Shorter hint |
| 2026-10-15 | SVG chart centred the first and last axis labels on the plot edges (clipped) and printed every label even when they overlapped ("SGD 20,000" ×10) | Measured label bounding boxes for all eight countries in the browser; screenshots alone were stale and showed nothing wrong | Tests first, then padded plot and thinned labels; re-measured: 0 overlaps in all eight |
| 2026-10-15 | Employees page swallowed a failed reference-data request (`catchError` → empty lists), so Add opened with no countries and no message; tables unnamed; layouts overflowed at 400 px | Deliberate UI audit: blocked the endpoint in the live app, ran a DOM accessibility check, narrowed the window and measured overflow | Tests first for the state and table names; then CSS wrap/scroll fixes verified by measurement |
| 2026-10-15 | A commit titled as the SPA-fallback test contained only its two resource files: the test class had not been written because its folder did not exist, and a failed `cat >` did not stop the chain | Checked `git show --stat` of the commit before moving on | Reset the unpushed commit, created the folder, committed the real test (now always check what a commit contains) |
| 2026-10-15 | Used bare `@WebMvcTest` for a static-resource test, which loads every controller and fails on their missing services; and asserted `/` with MockMvc, which does not follow Spring Boot's welcome-page forward | Test failures; read the root cause from the report | Restrict the slice to no controller; test `/index.html` in the slice and `/` against the running container |
| 2026-10-15 | Compose `app` service had no `image:` name, so `--no-build` looked for an image I had not tagged | Compose error "No such image" | Explicit `image: acme-salary:local` |
| 2026-10-15 | First security test expected the exact header `Basic realm="…"`; Spring Security 7 appends `, charset="UTF-8"` | Test run showed the actual header | Assert the realm prefix, which is the behaviour that matters |
| 2026-10-15 | Browser check of the authenticated app showed "Could not load the summary" and looked like a bug | Probed with `fetch`: browsers refuse requests when the page URL contains `user:password@`, so every relative API call failed. A test-method artifact, not an app bug | Primed the browser's login cache on a protected URL, then opened the app at its clean URL; full flow passed (list, insights, create 201, delete 204) |

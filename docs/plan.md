# Implementation plan

Planning notes for building ACME Salary Management. Written before coding; updated as the work evolves. Read together with [`requirements.md`](requirements.md), [`architecture.md`](architecture.md) and [`../CLAUDE.md`](../CLAUDE.md).

## Stack

Java 21 · Spring Boot 4 · Gradle (Kotlin DSL) · Spring Data JPA/Hibernate · Flyway · PostgreSQL · Angular (standalone, signals) · Angular Material · JUnit 5/AssertJ/Mockito/Testcontainers · GitHub Actions · Docker (single image) · Render + Neon Postgres.

## Working method

- Test-first: `test:` commit (red) → `feat:` commit (green) → optional `refactor:` commit.
- One behaviour per commit, all tests green before committing. Push after every few commits.
- Stop after each commit for human review.
- Record every AI mistake caught in [`ai-usage.md`](ai-usage.md).

## Steps

Status: ☐ todo · ◐ in progress · ☑ done

### Step 1 — Docs before code ☑
1. README skeleton, `.gitignore`, `.gitattributes`
2. `docs/requirements.md`
3. `docs/architecture.md` + ADRs 001–005
4. `CLAUDE.md` + `docs/ai-usage.md` + this plan

### Step 2 — Scaffold & CI ☑
5. Spring Boot backend in `backend/` (Web, Data JPA, Validation, Flyway, PostgreSQL, Actuator, Testcontainers). Base package `com.acme.salary`. Context-loads test passes.
6. Angular frontend in `frontend/` (standalone, routing, SCSS, Angular Material). Dev proxy `/api` → `localhost:8080`.
7. `docker-compose.yml` with PostgreSQL 16 for local dev; `application.yml` reads DB settings from env vars with local defaults.
8. `.github/workflows/ci.yml`: backend `./gradlew test` (Docker available for Testcontainers) + frontend `npm ci && npm test -- --watch=false` + `npm run build`.

### Step 3 — Employee core (TDD) ☑
9. `Money` value object (BigDecimal amount, `Currency`; rejects null/negative; scale 2; equality by value). Country → currency mapping (`Country` enum: US, GB, IN, DE, FR, CA, AU, SG).
10. Flyway `V1__create_employee.sql` (+ indexes from architecture.md) and `Employee` entity.
11. `EmployeeService` create/update: unique employee code & email, salary > 0, currency derived from country, hire date not in the future (injected `Clock`).
12. `EmployeeController` CRUD with `@WebMvcTest`; errors as RFC 7807 `ProblemDetail` via `GlobalExceptionHandler`; 404 for missing, 409 for duplicates, 400 for validation.
13. Paginated search/filter: `GET /api/employees?page&size&sort&search&country&department&jobTitle` using JPA Specifications; repository test with Testcontainers. `GET /api/reference-data` for dropdowns.

### Step 4 — Seed 10,000 employees ☑
14. `EmployeeGenerator` (pure, seeded `Random`): names from `first_names.txt` / `last_names.txt`; salary = title band × country factor ± bounded noise; tests for count, determinism, ranges, unique codes/emails.
15. `SeedRunner`: JDBC batch insert (batch 1,000), idempotent (skips if data exists; `--reset` option), runs via Spring profile `seed`. Record timing for `performance.md`.

### Step 5 — Insights ☑
16. `GET /api/insights/countries` — headcount, min, max, avg, median (`percentile_cont`) per country in local currency.
17. `GET /api/insights/countries/{code}/job-titles` — per-title stats within a country.
18. `GET /api/insights/countries/{code}/distribution` — histogram buckets (`width_bucket`).
19. `V2__create_exchange_rate.sql` (fixed, dated rates) + `GET /api/insights/summary` — org totals in USD, flagged approximate, with rate date.
20. `GET /api/employees/export.csv` — filtered CSV export (streamed).
Each query tested on a small known dataset (e.g. 5 salaries → known median).

### Step 6 — UI ☑
21. Employees page: Material table + paginator + sort (server-side), debounced search, country/department/title filters, sorting.
22. Add/edit dialog (reactive form, validation messages mirroring backend rules, server error display); delete with confirmation; CSV export button.
23. Insights dashboard: country comparison table, job-title drill-down, distribution chart, summary cards ("≈ USD, rates as of …").
24. Loading / empty / error states everywhere; component & service tests.

### Step 7 — Ship ◐
25. Multi-stage `Dockerfile` (node build → gradle build → JRE runtime), Angular served by Spring Boot.
26. Deploy to Render with Neon Postgres; basic auth via env vars (ADR 005); seed production.
27. `docs/performance.md` (seed timing, query timings at 10k, indexes, paging); finish `ai-usage.md`; final README (run locally, run tests, live URL, screenshots).
28. 3–5 min demo video: problem → list & filters → add/edit with validation → insights ("median engineer salary IN vs US") → tests/CI → one trade-off.

## Open questions / decisions log

- **2026-10-15 — Spring Boot 4.1 instead of 3.** Boot 3.x is out of open-source support and no longer on Spring Initializr. See [ADR 006](adr/006-spring-boot-4.md).
- **2026-10-15 — Branching.** Each push gets its own branch cut from `dev` and a PR into `dev`.
- **2026-10-15 — Tests pin `postgres:16-alpine`.** Not `latest`, so test runs are reproducible and match the local Compose database (item 7).
- **2026-10-15 — Angular Material instead of PrimeNG.** PrimeNG is now commercially licensed and needs a licence key. See [ADR 007](adr/007-angular-material-over-primeng.md).
- **2026-10-15 — Node 24.15+ required.** Angular 22 needs Node `^22.22.3 || ^24.15.0 || >=26`. Local dev uses Node 24 LTS (24.22.0); CI will pin Node 24.
- **2026-10-15 — Employee schema details (V1).** `varchar(2)`/`varchar(3)` instead of `char(n)` (PostgreSQL discourages `char(n)`; the Java enum restricts values). Identity column instead of `bigserial`. Email unique case-insensitively. No separate `country_code` index — the `(country_code, job_title)` index covers it.
- **2026-10-15 — No `lower(full_name)` / trigram index.** A b-tree index cannot serve `LIKE '%term%'` search. Measured in item 13: search over 10k rows takes 13.8 ms in PostgreSQL and ≤ 56 ms end to end, far under the 300 ms target. See [performance.md](performance.md).
- **2026-10-15 — List API returns our own `PageResponse`**, not Spring's `Page` (whose JSON is not a stable contract). Sorting is whitelisted (unknown property → 400), defaults to name with `id` as tie-breaker; page size capped at 100.
- **2026-10-15 — "Hire date not in the future" uses today in UTC** (injected `Clock`). Known limitation: between 00:00 and 05:30 IST a same-day hire in India is still "tomorrow" in UTC and is rejected. Acceptable for v1; revisit if HR reports it.
- **2026-10-15 — Seed via JDBC batches with `reWriteBatchedInserts`.** 10k employees in ~0.9 s (vs ~2.4 s without the driver flag). Fixed random seed 42; idempotent unless `--reset`. See [performance.md](performance.md).
- **2026-10-15 — Exact median without `percentile_cont`.** `percentile_cont` only accepts `double precision`, which would push salaries through floating point. Median = mean of `percentile_disc(0.5)` ascending and descending (lower and upper middle values), staying in `numeric`; average and median rounded half-up to cents.
- **2026-10-15 — Histogram with "nice" bucket edges.** Bucket width = (max − min) / 10 rounded up to 1, 2, 2.5 or 5 × 10ⁿ (whole cents), first edge rounded down to a multiple of it, so HR sees ₹5L or £10k steps instead of edges like 16,234.57. Counts via `width_bucket`; empty buckets returned with 0.
- **2026-10-15 — Exchange rates from the ECB, not invented.** V2 holds the European Central Bank euro reference rates of 2026-10-14, converted to USD per unit (8 places), so "rates as of" in the UI is a real, citable date. New rates = new migration.
- **2026-10-15 — USD summary converts per currency, rounds once.** Salaries are summed per currency in SQL; each total is converted exactly (`numeric × rate`), the USD values are added and rounded half-up to cents once. A currency without a rate fails loudly (`MissingExchangeRateException`) rather than being left out of the total. Verified on the seed: API total equals an independent `sum(salary × usd_rate)` to the cent.
- **2026-10-15 — CSV export: streamed, Excel-friendly, injection-safe.** Same filters/sort as the list; read in pages of 1,000 (flat memory); UTF-8 BOM so Excel shows accents; RFC 4180 quoting; text starting with `= + - @`, tab or CR prefixed with `'` (OWASP CSV injection) because HR opens the file in Excel. All 10k rows in ~0.3 s.
- **2026-10-15 — Employees page state as an RxJS query stream.** The query (page, size, sort, search, filters) is a `BehaviorSubject` exposed to the template as a signal; every change flows through one `switchMap` to `EmployeeApi`, cancelling stale requests. Signals hold view state (loading / loaded / error, result). Any change but paging returns to page 1.
- **2026-10-15 — Distribution chart as plain SVG, no chart library.** One bar chart does not justify a dependency (ADR 007 left this open for item 23). `DistributionChart` draws bars scaled to the largest bucket, with a `<title>` per bar and an `aria-label` summary for screen readers. Checked on all eight countries in the browser: the plot is inset so outer labels are not clipped, and axis labels are thinned (every other one) when the widest would not fit its bucket (Singapore's "SGD 20,000").
- **2026-10-15 — Insights page loads each section independently.** Summary, country table and the selected country's details (job titles + distribution, fetched together) each have their own loading / error-with-Retry state, so one failing endpoint does not blank the page. The USD cards are labelled "≈" with the rate date; the country table stays in local currency with a note saying so.
- **2026-10-15 — UI states audit (item 24).** Every screen already had loading / empty / error states; the audit looked for what was *missing*. Found and fixed: (1) a failed `/api/reference-data` left the Add dialog with no countries and no explanation — reference data now has its own state, Add/Edit are disabled until it loads, and a warning offers Retry (a failed *refresh* after a save keeps the options already known); (2) tables had no accessible names; (3) at 400 px wide the insights page stretched to ~690 px and the toolbar and employees header overflowed — wide tables now scroll inside their own box and the toolbar/header wrap. Desktop remains the primary target (requirements §4); there is no mobile-specific design.
- **2026-10-15 — One image, Angular served by Spring Boot (ADR 001 in practice).** The Dockerfile builds the Angular app, copies `dist/frontend/browser` into the jar's `static/`, and runs on `eclipse-temurin:21-jre-alpine` as a non-root user. `SpaWebConfiguration` serves real files as they are and gives other extension-less paths `index.html`, so `/employees` and `/insights` survive a refresh; `/api`, `/actuator` and missing files keep their 404. The HTML shell is `Cache-Control: no-cache`; assets are revalidated too (simple and safe; hashed-asset long caching is a possible later optimisation).
- **2026-10-15 — Proxy-aware and compressed.** `server.forward-headers-strategy: framework` so a TLS-terminating host (Render) gets `https://` in `Location` headers (verified with `X-Forwarded-Proto`); response compression on. Tests are not run during the image build (they need Docker for Testcontainers and run in CI).
- **2026-10-15 — Basic auth for the hosted demo (item 26, ADR 005).** `spring-boot-starter-security` is the one new dependency: audited basic-auth handling and security headers instead of hand-rolled code. One shared login from `APP_SECURITY_*` guards the app, API and CSV export; only `/actuator/health` is open. Unset locally (open), but the `prod` profile refuses to start without it, so a forgotten variable cannot publish salary data. Stateless, CSRF off (JSON-only writes, no CORS preflight answered), constant-time compare via SHA-256 digests (BCrypt would add ~100 ms CPU per request).
- **2026-10-15 — Deployment is prepared, not performed, by the agent.** `render.yaml` (Docker web service, `prod` profile, secrets `sync: false`), `application-prod.yml` (small Hikari pool, connection lifetimes shorter than Neon's idle close, 30 s connect timeout for a paused database) and [deployment.md](deployment.md). Creating the Render and Neon accounts and pasting five secrets are the owner's steps; the seed runs once from a laptop (`--spring.profiles.active=seed`), so production gets no seeding code path. Render's builder and Neon's SSL/wake behaviour cannot be tested from the repository and are listed as such.


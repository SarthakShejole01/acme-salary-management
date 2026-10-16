# Performance notes

Targets from [`requirements.md`](requirements.md): list / search / filter **< 300 ms**, insight queries **< 500 ms**, at 10,000 employees.

## Summary against the targets

| Requirement (§5) | Target | Measured | Where |
|---|---|---|---|
| List, search and filter | < 300 ms | 20–56 ms | [Employee list](#employee-list-and-search-plan-item-13) |
| Insight queries | < 500 ms | 9–18 ms | [Insights](#insights-plan-items-1620) |
| Seeding 10,000 employees | seconds, deterministic | 0.9 s, identical on every run | [Seeding](#seeding-10000-employees-plan-item-15) |
| Memory on a small host | n/a | 279 MB peak in a 512 MB container | [Docker image](#docker-image-plan-item-25) |

**Method, for all sections.** Local laptop (Windows 11, Ryzen 5 5625U), PostgreSQL 16 in Docker, the 10,000 seeded employees, end-to-end HTTP timings with `curl`, best of five after warm-up. These are local, single-user figures: they show the queries and indexes are not the bottleneck, not what a loaded production host would do. Nothing here was measured on Render or Neon.

## Employee list and search (plan item 13)

**Setup.** Local laptop (Windows 11, Ryzen 5 5625U), PostgreSQL 16 in Docker (`docker compose`), backend from `bootJar`. 10,000 synthetic employees inserted with `generate_series` (8 countries, 5 departments, 5 job titles). Times are end-to-end HTTP (`curl`), best of 5 after 3 warm-up requests, default page size 20.

| Request | Time |
|---------|------|
| `GET /api/employees` (first page, name order) | 35 ms |
| `GET /api/employees?search=sharma` (839 matches) | 56 ms |
| `GET /api/employees?search=user9999` (1 match) | 33 ms |
| `GET /api/employees?search=zzz-no-match` | 34 ms |
| `GET /api/employees?country=IN&jobTitle=Software Engineer` | 27 ms |
| `GET /api/employees?sort=salary,desc&page=40` | 35 ms |
| `GET /api/reference-data` | 20 ms |
| `GET /api/employees/export.csv` (all 10,000 rows, 1.1 MB, streamed in pages of 1,000) | 292 ms |

**Search query plan** (`lower(full_name|employee_code|email) like '%sharma%'`, `order by full_name, id limit 20`): sequential scan + top-N heapsort, **13.8 ms** execution.

**Decision.** No `pg_trgm` / `lower(full_name)` index. A b-tree index cannot serve `LIKE '%term%'`, and a full scan of 10k rows is far inside the 300 ms budget. Revisit with a trigram GIN index (new Flyway migration) if the data grows by an order of magnitude.

**What keeps it cheap.** Server-side paging with a page-size cap of 100; one count query and one page query per request; `(country_code, job_title)` and `(department)` indexes for the exact-match filters.

## Seeding 10,000 employees (plan item 15)

`java -jar salary.jar --spring.profiles.active=seed --reset` against the local Docker PostgreSQL 16. Time is what `SeedRunner` logs: generation + truncate + insert, excluding JVM start-up.

| Insert strategy | Time (3 runs) |
|-----------------|---------------|
| JDBC batch of 1,000, plain | 2,393 – 2,545 ms |
| JDBC batch of 1,000 + driver `reWriteBatchedInserts=true` | **861 – 901 ms** |

First run into an empty database (cold JVM): 3,410 ms. A second run without `--reset` is skipped.

**Choices.** JDBC batches instead of JPA `saveAll` (no per-entity persistence-context overhead, one round trip per 1,000 rows); one transaction, so a failed seed leaves no partial data; `reWriteBatchedInserts` turns each batch into multi-row `INSERT` statements and is now on by default in `application.yml`.

## Insights (plan items 16–20)

Same setup, against the 10,000 seeded employees. End-to-end HTTP, best of 5 after warm-up.

| Request | Time |
|---------|------|
| `GET /api/insights/countries` (8 countries: headcount, min, max, avg, exact median) | 18 ms |
| `GET /api/insights/countries/IN/job-titles` (19 titles) | 10 ms |
| `GET /api/insights/countries/US/job-titles` | 10 ms |
| `GET /api/insights/countries/IN/distribution` (7 buckets of ₹5 lakh) | 11 ms |
| `GET /api/insights/countries/US/distribution` | 12 ms |
| `GET /api/insights/summary` (USD totals; 7 currency rows converted) | 9 ms |

Country and job-title stats are one `GROUP BY` query each (`percentile_disc` for the median); the distribution is two small queries (min/max, then `width_bucket` counts). No employee rows are loaded into the application.

## Docker image (plan item 25)

Built and run locally (Docker Desktop, PostgreSQL 16 container, the 10,000 seeded employees).

| Measure | Result |
|---------|--------|
| Image size | 393 MB (`eclipse-temurin:21-jre-alpine` plus a 53.6 MB application jar that includes the Angular build) |
| Cold build (no cache) | about 7.5 min, of which 4 min is the first Gradle dependency download; later builds reuse the dependency layer |
| Start-up to healthy | 13.4 s |
| Memory under load | 279 MB of a **512 MB** limit (12 concurrent requests incl. four full 10,000-row CSV exports, three rounds); no OOM kill, no restarts |
| JS bundle on the wire | 407 KB raw → 118 KB with gzip (`server.compression`) |

JVM flags for a small instance: `-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k` (override with `JAVA_TOOL_OPTIONS`).

## Hosted instance (Render free web service + Neon free database)

Measured on 2026-10-16, from a laptop, **without a login**, so only the open health endpoint and the 401 challenge can be timed; no application endpoint was measured on the hosted instance.

| Measure | Result |
|---------|--------|
| Seeding 10,000 employees into Neon from a laptop | 13.9 s (0.9 s against a local database; the difference is network latency) |
| Container start with Neon waking from idle | 44.6 s |
| `GET /actuator/health` (TLS included), two requests | 0.92 s, then 0.67 s |
| `GET /employees` without a login (401) | 0.49 s |

The roughly half-second floor on every request is the round trip from the laptop to the hosting region, not application time: compare the 9–56 ms local figures above. The free plans sleep when idle (see [deployment.md](deployment.md)), so a first request after a pause is much slower.


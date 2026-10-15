# Performance notes

Targets from [`requirements.md`](requirements.md): list / search / filter **< 300 ms**, insight queries **< 500 ms**, at 10,000 employees.

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

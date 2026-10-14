# ADR 004 — Compute insights in the database, not in Java

**Status:** Accepted

## Context

Insights (min/max/avg/median per country, per-title stats, histograms) span all 10,000 employees.

## Decision

Implement insights as SQL aggregate queries (`GROUP BY`, `percentile_cont`, `width_bucket`) behind an `InsightsQueryRepository`, returning small result DTOs. Back them with indexes on `country_code` and `(country_code, job_title)`.

## Reasons

- Moves kilobytes over the wire instead of 10k entities; no Hibernate hydration cost.
- The database is designed for this and stays fast as data grows.
- Queries are easy to verify with a small, known dataset in Testcontainers tests (e.g. 5 salaries → known median).

## Consequences

- Some logic lives in SQL, so it is covered by repository tests rather than pure unit tests.
- Pre-computed/materialised views are unnecessary at 10k rows; revisit if data grows by orders of magnitude.

# ADR 002 — PostgreSQL instead of SQLite

**Status:** Accepted

## Context

The brief allows any relational database, e.g. SQLite. The app must be deployed and must answer statistical questions (medians, distributions) over 10,000 rows.

## Decision

Use PostgreSQL everywhere: local development (Docker Compose), integration tests (Testcontainers) and production (managed Postgres).

## Reasons

- **Deployment:** free/low-cost hosts use ephemeral disks; a SQLite file is wiped on every redeploy. Managed Postgres persists.
- **Analytics:** Postgres has `percentile_cont` for exact medians and `width_bucket` for histograms; SQLite has neither. *(Amended 2026-10-15: the median uses `percentile_disc` so it stays exact `numeric`; see [plan decisions](../plan.md#open-questions--decisions-log).)*
- **Test fidelity:** running the same engine in tests and production means aggregate SQL is tested for real, not against an H2 dialect that behaves differently.

## Consequences

- Docker is required to run integration tests locally. Unit tests (the majority) need nothing.
- Slightly more setup than a single file — mitigated by `docker compose up`.

# ACME Salary Management

[![CI](https://github.com/SarthakShejole01/acme-salary-management/actions/workflows/ci.yml/badge.svg?branch=dev)](https://github.com/SarthakShejole01/acme-salary-management/actions/workflows/ci.yml)

Web-based salary management for ACME's HR team: maintain salary data for ~10,000 employees across countries, and answer questions about how the organisation pays people — without spreadsheets.

> **Status:** in progress. This README grows with the project.

## Why this exists

ACME's HR manager currently maintains salaries in Excel files. That is slow, error-prone, and makes questions like *"What is the median salary for engineers in India vs Germany?"* a manual exercise. This application replaces the spreadsheets with a single source of truth and built-in pay insights.

See [`docs/requirements.md`](docs/requirements.md) for scope and the deliberate exclusions.

## Tech stack

| Layer      | Choice |
|------------|--------|
| Backend    | Java 21, Spring Boot 4, Spring Data JPA (Hibernate), Flyway, Gradle (Kotlin DSL) |
| Database   | PostgreSQL |
| Frontend   | Angular (standalone components, signals), TypeScript, Angular Material |
| Testing    | JUnit 5, AssertJ, Mockito, Testcontainers · Angular unit tests |
| CI/CD      | GitHub Actions · Docker |

## Repository layout

```
backend/    Spring Boot API (Gradle, Java 21)
frontend/   Angular application (Angular 22, Material)
docs/       Requirements, architecture, decision records, AI usage notes
CLAUDE.md   Working agreement for the AI coding agent
```

## Getting started

**Prerequisites:** Java 21, Node 24.15+ (Angular 22), Docker Desktop.

```bash
# 1. Database — PostgreSQL 16 on localhost:5433 (5433 avoids clashing with a local install)
docker compose up -d

# 2. Backend — http://localhost:8080 (health: /actuator/health)
cd backend && ./gradlew bootRun

# 3. Frontend — http://localhost:4200, proxies /api to the backend
cd frontend && npm ci && npm start
```

**Seed data** — 10,000 realistic, deterministic employees (same data every run):

```bash
cd backend && ./gradlew bootRun --args='--spring.profiles.active=seed'          # skips if employees exist
cd backend && ./gradlew bootRun --args='--spring.profiles.active=seed --reset'  # replaces all employees
```

Defaults work out of the box. To override database settings, copy `.env.example` to `.env` (git-ignored) or set the `DB_*` environment variables.

**Tests:** `cd backend && ./gradlew test` (needs Docker running — Testcontainers starts its own PostgreSQL) · `cd frontend && npm test -- --watch=false`

## Documentation

- [Requirements](docs/requirements.md)
- [Architecture](docs/architecture.md)
- [Architecture decision records](docs/adr/)
- [Performance notes](docs/performance.md)
- [How AI tools were used](docs/ai-usage.md)

## Live demo & video

_To be added._

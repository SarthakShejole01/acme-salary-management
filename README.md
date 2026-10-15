# ACME Salary Management

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
| Frontend   | Angular (standalone components, signals), TypeScript, PrimeNG |
| Testing    | JUnit 5, AssertJ, Mockito, Testcontainers · Angular unit tests |
| CI/CD      | GitHub Actions · Docker |

## Repository layout

```
backend/    Spring Boot API (Gradle, Java 21)
frontend/   Angular application (to be added)
docs/       Requirements, architecture, decision records, AI usage notes
CLAUDE.md   Working agreement for the AI coding agent
```

## Getting started

_To be added once the backend and frontend are scaffolded._

## Documentation

- [Requirements](docs/requirements.md)
- [Architecture](docs/architecture.md)
- [Architecture decision records](docs/adr/)
- [How AI tools were used](docs/ai-usage.md)

## Live demo & video

_To be added._

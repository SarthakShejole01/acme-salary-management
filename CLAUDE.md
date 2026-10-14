# Working agreement for AI coding agents

This file tells AI assistants (Claude Code, Cursor, Copilot) how to work in this repository. Read `docs/requirements.md` and `docs/architecture.md` before making changes.

## How we work

1. **Test first.** Write a failing test, make it pass with the simplest code, then refactor. Never write production code without a test that needs it.
2. **Small steps.** One behaviour per change. Each change should be committable on its own with all tests green.
3. **Stay in scope.** Build only what `docs/requirements.md` lists. If something seems missing, ask — do not add features speculatively.
4. **Explain trade-offs.** When there is more than one reasonable approach, state the options and the choice before coding. Significant decisions get an ADR in `docs/adr/`.

## Backend conventions (Java 21, Spring Boot 3)

- Package by feature: `employee`, `insights`, `currency`, `seed`, `common`.
- **Money is `BigDecimal` wrapped in the `Money` value object. Never `double` or `float`.**
- Never add or average amounts in different currencies without converting.
- Controllers are thin; business rules live in services; entities never leave the service layer (use DTOs/records).
- Constructor injection only; no field `@Autowired`.
- Validate input with Bean Validation on request DTOs; errors return RFC 7807 `ProblemDetail`.
- Schema changes only via new Flyway migrations — never edit an applied migration.
- Aggregations in SQL, not by loading entities into memory.
- Inject `java.time.Clock` instead of calling `LocalDate.now()` directly.

## Frontend conventions (Angular, TypeScript strict)

- Standalone components, signals for state, RxJS for HTTP/streams.
- Feature folders under `features/`; HTTP access only through services in `core/`.
- Paging, sorting and filtering are done server-side.
- Every view handles loading, empty and error states.

## Tests

- Fast and deterministic: no real clock, no unseeded randomness, no sleeps, no network.
- Name tests by behaviour: `rejects_negative_salary`, `median_of_even_count_averages_middle_values`.
- Arrange / Act / Assert, one behaviour per test, AssertJ for assertions.
- Unit tests for domain logic; `@WebMvcTest` for HTTP contracts; Testcontainers (PostgreSQL) for SQL queries. No H2.

## Commits

Conventional style, small and frequent: `test: …`, `feat: …`, `refactor: …`, `fix: …`, `docs: …`, `chore: …`, `ci: …`.

## Don'ts

- Don't disable, skip or delete failing tests to make the build pass.
- Don't add dependencies without saying why.
- Don't commit secrets; configuration comes from environment variables.

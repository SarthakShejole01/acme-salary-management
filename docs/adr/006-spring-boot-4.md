# ADR 006 — Spring Boot 4 instead of Spring Boot 3

**Status:** Accepted (amends ADR 001)

## Context

The original plan and ADR 001 named Spring Boot 3. When the backend was scaffolded (October 2026), Spring Boot 3.x had reached the end of open-source support (June 2026) and Spring Initializr no longer offered it. The current GA release was 4.1.1.

## Decision

Use Spring Boot 4.1.x on Java 21, with Gradle (Kotlin DSL).

## Consequences

- The backend receives security patches for the life of this project; a reviewer will not see an unsupported framework.
- Spring Boot 4 brings Jackson 3, Testcontainers 2 and finer-grained starters (e.g. `spring-boot-starter-flyway`, `spring-boot-starter-webmvc` and matching `*-test` starters). Some tutorials written for Boot 3 need small adjustments.
- Java 21 remains the baseline; nothing in this project needs a newer JDK.
- Rejected: Spring Boot 3.5.x. It matches the docs as first written, but it is outside free support and would have to be set up by hand.

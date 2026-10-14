# ADR 001 — Single deployable: Spring Boot serving the Angular app

**Status:** Accepted

## Context

The brief requires an end-to-end, deployed application with a Java backend and an Angular UI. Reviewers need one URL that just works. The team is one engineer.

## Decision

Build the Angular app at image-build time and serve its static files from Spring Boot. Ship one Docker image containing both. Backend uses Spring Boot 3 on Java 21 with Gradle (Kotlin DSL).

Spring Boot over Micronaut: both fit the role; Spring has the larger ecosystem (Spring Data, Testcontainers support, springdoc) and is faster to deliver with. Micronaut's faster startup does not matter at this scale.

## Consequences

- One deployment, one URL, no CORS configuration in production.
- Frontend and backend release together — acceptable for a single team and a single product.
- During development the two run separately (`ng serve` proxies `/api` to Spring Boot).
- If the UI ever needs independent scaling or a CDN, it can be split out without code changes.

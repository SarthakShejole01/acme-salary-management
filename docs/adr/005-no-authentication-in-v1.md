# ADR 005 — No user accounts in v1; protect the demo with basic auth

**Status:** Accepted

## Context

There is one persona (the HR Manager). The brief does not ask for access control, but salary data is sensitive and the app will be publicly deployed.

## Decision

- No user model, login screen or roles in v1.
- Protect the deployed instance with a single HTTP basic-auth credential supplied via environment variables (never committed).

## Consequences

- No audit of *who* changed data (see requirements: audit trail is out of scope).
- Before real use, replace with SSO (e.g. the org's identity provider) and role-based access — listed in the roadmap.

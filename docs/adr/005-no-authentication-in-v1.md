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

## Implementation (added 2026-10-15)

Built with Spring Security (`spring-boot-starter-security`, the one dependency added for this), in [`SecurityConfiguration`](../../backend/src/main/java/com/acme/salary/common/SecurityConfiguration.java):

- **One login, whole application.** `APP_SECURITY_USERNAME` / `APP_SECURITY_PASSWORD` protect the Angular app, the API and the CSV export. Only `/actuator/health` is open, because the hosting platform polls it. The browser's own login prompt is the "login screen"; after one login it attaches the credentials to every same-origin request, so the Angular code needs no auth logic.
- **Fail closed.** Without credentials the app runs open, which is wanted locally and in tests. The `prod` profile (set by `render.yaml`) refuses to start without both values, and a half-set login always fails to start, so a hosted instance cannot go live unprotected by a forgotten variable.
- **Stateless, CSRF protection off.** A browser re-sends cached basic-auth credentials to any site that asks, which is the classic CSRF risk. Here every write needs a JSON body: a cross-site page cannot send one without a CORS preflight, and the API never answers preflights; there are no form endpoints. Reads are not state-changing.
- **Constant-time comparison.** The password is compared via SHA-256 digests with `MessageDigest.isEqual`. BCrypt was rejected: the credential lives in an environment variable (nothing to protect at rest), and BCrypt would add about 100 ms of CPU to every request because the browser sends the login with each one.
- **Safe defaults from Spring Security:** `X-Content-Type-Options`, `X-Frame-Options: DENY`, and `Cache-Control: no-store` on API responses, so salary data is not kept in browser or proxy caches.

Further consequences: no lockout or rate limiting on failed attempts (a long random password is the control); credentials are only safe over HTTPS, which the host provides; rotating the login is an environment-variable change and a redeploy. Walkthrough: [deployment.md](../deployment.md).

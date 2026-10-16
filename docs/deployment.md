# Deployment: Render + Neon

The application ships as **one Docker image** (Angular and Spring Boot together, see the [`Dockerfile`](../Dockerfile)). Render runs the image; Neon provides the PostgreSQL database. A shared HTTP basic-auth login protects everything except the health check ([ADR 005](adr/005-no-authentication-in-v1.md)).

```
browser ──https──▶ Render (Docker web service, profile "prod")  ──jdbc/ssl──▶  Neon PostgreSQL
                   login prompt → Angular app + /api + CSV export
```

You do three things; everything else is already in the repository (`render.yaml`, `application-prod.yml`, the seed command).

| # | You | Takes |
|---|-----|-------|
| 1 | Create a Neon database and note four values | 5 min |
| 2 | Load the 10,000 demo employees into it (one command) | 2 min |
| 3 | Create the Render service from `render.yaml` and paste five values | 15 min, mostly the first build |

## Before you start

- Accounts on [neon.com](https://neon.com) and [render.com](https://render.com), both with free plans. Creating them is up to you; nothing here needs a payment method.
- Merge `dev` into `main`. The blueprint deploys the **`main`** branch.
- Choose the application login now: a username (e.g. `hr`) and a **long random password** (16+ characters, from a password manager). It exists only in Render's environment settings; it is never committed.

## 1. Neon: the database

1. Create a project: **PostgreSQL 16** (or newer: the live database runs 18.6 and works unchanged), region **AWS US West 2 (Oregon)**, to sit next to Render's default Oregon region.
2. Open **Connection Details**. Switch **Connection pooling off** so you get the *direct* host (it does **not** contain `-pooler`). Flyway and the JDBC driver expect a direct connection.
3. Write down:
   - **host**, e.g. `ep-quiet-sun-123456.us-west-2.aws.neon.tech`
   - **database**, usually `neondb`
   - **role** (user), usually `neondb_owner`
   - **password**
4. Build the JDBC URL (the user and password are *not* part of it):

   ```
   DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require
   ```

## 2. Load the demo data (once, from your computer)

The seed is a separate run of the same image with the `seed` profile. It creates the tables (Flyway) and inserts 10,000 deterministic employees in about a second, then exits. It skips itself if employees already exist.

Build the image once (needs Docker running; the first build takes several minutes):

```bash
docker build -t acme-salary:local .
```

Then run the seed against Neon (PowerShell shown; in bash use `\` instead of the backtick):

```powershell
docker run --rm `
  -e DB_URL='jdbc:postgresql://<host>/<database>?sslmode=require' `
  -e DB_USERNAME='<role>' `
  -e DB_PASSWORD='<password>' `
  acme-salary:local --spring.profiles.active=seed
```

Expected last line: `Seeded 10000 employees in … ms`. Running it again prints `Skipped seeding: 10000 employees already exist`.

> **Never add `--reset` against the live database once real data has been entered**: it deletes every employee first.

## 3. Render: the web service

1. Render dashboard → **New → Blueprint** → connect the GitHub repository → branch `main`. Render reads [`render.yaml`](../render.yaml).
2. It asks for the five values marked as secrets:

   | Variable | Value |
   |----------|-------|
   | `DB_URL` | the JDBC URL from step 1 |
   | `DB_USERNAME` | the Neon role |
   | `DB_PASSWORD` | the Neon password |
   | `APP_SECURITY_USERNAME` | the login username you chose |
   | `APP_SECURITY_PASSWORD` | the login password you chose |

   The blueprint itself sets `SPRING_PROFILES_ACTIVE=prod` and `PORT=8080`.
3. Apply. The first build compiles the frontend and backend inside Render and takes roughly 8–10 minutes; later deploys reuse cached layers and are faster. Render marks the service live once `/actuator/health` answers.

## 4. Check it

1. Open the service URL (`https://acme-salary-management-….onrender.com`). The browser asks for the login; after you enter it you should see **Employees · 10,000**.
2. Open **Insights**; select a country; the chart and job-title table appear.
3. `https://<your-service>/actuator/health` answers `{"status":"UP"}` **without** a login (the platform's health check). Nothing else does.
4. Paste the live URL into the README (plan item 27).

## Things that behave differently on the free plans

- **Cold starts.** A free Render web service sleeps after a period of inactivity and takes about a minute to wake on the next request; Neon also pauses an idle database and wakes within a second or so. Open the app a couple of minutes before a demo. The connection pool is configured for this (`application-prod.yml`).
- **Changing the login.** Edit the two `APP_SECURITY_*` variables in Render → Environment; the service redeploys.
- **HTTPS only.** Basic auth sends the password with every request, which is safe only over TLS. Render serves the app over HTTPS; always use the `https://` address.
- **No lockout or rate limiting.** One shared login, no audit trail: that is the deliberate v1 trade-off (ADR 005). A long random password is the protection; SSO and roles are the first roadmap item before real use.

## If something goes wrong

| Symptom | Likely cause and fix |
|---------|----------------------|
| Service fails at start with *"The prod profile requires a login"* | `APP_SECURITY_USERNAME` / `APP_SECURITY_PASSWORD` are missing or only one is set. Add both and redeploy. This is the intended safety net. |
| *"Connection to … refused"* / *"SSL"* errors | `DB_URL` must end with `?sslmode=require` and use the host **without** `-pooler`. |
| *"password authentication failed"* | `DB_USERNAME` / `DB_PASSWORD` are the Neon *role* values, not your Neon account login. |
| Build fails with an out-of-memory error | Render's free builder is small. Fallback: build the image locally, push it to a registry (for example GitHub Container Registry), and point Render at the image instead of the Dockerfile. |
| App is empty / *"Could not load employees"* | The seed has not run against this database (step 2), or a different database was seeded. |
| Browser keeps asking for the login | Wrong username or password in Render's environment; the browser reuses the last answer until you close the tab. |

## Deployed instance (2026-10-16)

Live at <https://acme-salary-management-grfk.onrender.com>. Observed on the real services:

- The seed against Neon (PostgreSQL 18.6, direct connection, `sslmode=require&channelBinding=require`) applied both Flyway migrations and loaded 10,000 employees in 13.9 s. That is network latency from a laptop; the same seed takes 0.9 s against a local database.
- Without a login: `/actuator/health` answers `UP` (the health check includes a database check, so Render reached Neon); `/employees`, `/api/employees` and the CSV export answer 401 with a `Basic` challenge; HTTP redirects to HTTPS; responses carry `nosniff`, `X-Frame-Options: DENY` and HSTS.
- The logged-in experience is checked by the owner with the steps in section 4.

## What was verified, and what could not be

Verified on the real image, locally against PostgreSQL 16: the `prod` profile refuses to start without a login (both when neither and when only one value is set); with a login, every path except `/actuator/health` answers 401 without it and 200 with it; the Angular app, a create and a delete work in a browser after logging in; the `seed` profile runs inside the image and exits; the image runs within 512 MB under load ([performance.md](performance.md)).

**Not verifiable from this repository:** Render's build environment (memory, time), and Neon's SSL and wake-from-pause behaviour. Both follow the providers' documented defaults; the troubleshooting table above covers the likely failures.

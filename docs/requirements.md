# Requirements — ACME Salary Management (v1)

## 1. Goal

Replace the Excel workbooks that ACME's HR team uses to manage salaries for ~10,000 employees across several countries with a web application that:

1. is the **single source of truth** for employee salary data, and
2. lets the HR Manager **answer questions about how the organisation pays people** in seconds instead of hours of spreadsheet work.

**Persona:** the HR Manager — comfortable with Excel, not technical, responsible for salary accuracy and for answering pay questions from leadership.

**Questions v1 must answer** (these drive the feature set):

- What is the min / max / average / median salary in each country?
- What does a given job title earn in a given country?
- How many people do we employ per country and department?
- How are salaries distributed — where are the outliers?
- Roughly what is our total annual salary cost, in one currency?

## 2. Scope & features (in v1)

| # | Feature | Notes |
|---|---------|-------|
| F1 | **Employee records** — create, view, edit, delete | Fields: employee code, full name, email, job title, department, country, annual base salary, currency, employment type, hire date. Validated server-side and in the form. |
| F2 | **Find employees** — paginated list with search and filters | Search by name / code / email; filter by country, department, job title; sort by any column. Must stay fast at 10k rows (server-side paging). |
| F3 | **Country pay insights** | Headcount, min, max, average, median salary per country, in local currency. |
| F4 | **Job-title pay insights** | Min, max, average, headcount per job title within a selected country. |
| F5 | **Salary distribution** | Histogram of salaries for a selected country. |
| F6 | **Org-wide summary** | Total headcount, total annual salary cost and averages normalised to USD, clearly labelled *approximate*. |
| F7 | **CSV export** | Export the currently filtered employee list — HR still shares data in spreadsheets. |
| F8 | **Seed data** | Deterministic script generating 10,000 realistic employees from first/last-name lists. |

**Key domain rules**

- Salary = **annual gross base salary**, stored in the employee's **local currency** as an exact decimal (never floating point).
- Currency is derived from country (e.g. IN → INR, DE → EUR), removing a whole class of data-entry errors.
- Salaries in different currencies are **never** aggregated without conversion. Per-country insights use local currency; cross-country figures use a fixed exchange-rate table.
- Employee code and email are unique.

## 3. Deliberately out of scope (and why)

| Left out | Reasoning |
|----------|-----------|
| **Authentication & roles** | One persona, and the brief does not mention access control. The deployed demo is protected with a single shared login (HTTP basic auth) because salary data is sensitive. Proper SSO/RBAC is the first thing to add before real use. |
| **Excel/CSV import** | The most valuable next feature given the pain point, but robust import (column mapping, validation reports, partial failures) is a feature in its own right. v1 proves the data model; the seed script covers the initial load. **First item for v2.** |
| **Salary history & audit trail** | Important for compliance ("who changed this salary, when?") but adds a temporal model. v1 records `created_at` / `updated_at` only. |
| **Live exchange rates** | Adds an external dependency and makes numbers change between page loads. For "how do we pay people" analysis, a fixed, dated rate table is sufficient and reproducible. |
| **Bonuses, equity, allowances, benefits** | v1 covers base salary only — the field HR currently tracks. Total compensation is a separate model. |
| **Payroll, tax, payslips** | A different product (payroll processing) with country-specific legal rules; not in the problem statement. |
| **Approval workflows** | No second persona (approver) defined; would be speculative. |
| **Org hierarchy / managers** | Not needed for the v1 questions. |

## 4. Assumptions

- ~10,000 employees, a handful of countries (~8), dozens of job titles. Data fits comfortably in a single relational database.
- A single HR Manager uses the system at a time; low write concurrency.
- Desktop browser is the primary device.

## 5. Quality bar / success criteria

- Employee list, search and filters respond in **< 300 ms** on 10k employees.
- Insight queries respond in **< 500 ms** (aggregations run in the database, not in application memory).
- Seeding 10,000 employees completes in **seconds**, and produces identical data on every run.
- Core rules (money handling, validation, insight calculations, seed generation) are covered by **fast, deterministic unit tests**; the full suite runs in CI on every push.
- Every screen has loading, empty and error states.

## 6. Roadmap after v1

1. Excel/CSV import with validation report
2. Authentication & role-based access
3. Salary change history and audit log
4. Pay-band definitions and "employees outside band" report
5. Pay-equity views (e.g. by gender/level), with appropriate privacy safeguards

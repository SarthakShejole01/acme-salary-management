# ADR 003 — Store salaries in local currency; convert with fixed rates

**Status:** Accepted

## Context

Employees are paid in their country's currency. HR asks both per-country questions ("median salary in India") and org-wide questions ("total salary cost").

## Decision

- Store each salary as an exact decimal (`numeric(14,2)` / `BigDecimal`) **in local currency**, with its ISO currency code. Model it as a `Money` value object.
- Derive currency from country, so the two can never disagree.
- Per-country insights are reported in local currency — no conversion error.
- Cross-country figures convert to USD using a **fixed, dated exchange-rate table**, and the UI labels them *approximate*.
- Never sum or average amounts of different currencies directly.

## Alternatives considered

- **Store everything in USD:** loses the true contractual amount; changes whenever rates move.
- **Live FX API:** external dependency, non-deterministic numbers and tests, rate-limit/outage risk — for no gain in a "how do we pay" analysis.
- **`double` for money:** rounding errors; rejected outright.

## Consequences

- USD totals are approximate and dated; the effective date is shown in the UI.
- Updating rates is a data change (migration), not a code change.

-- Fixed, dated rates for converting local salaries to USD in org-wide figures (ADR 003).
-- 1 unit of currency = usd_rate USD. Updating rates means a new migration, never editing this one.
create table exchange_rate (
    currency       varchar(3)     primary key,
    usd_rate       numeric(18, 8) not null,
    effective_date date           not null,
    constraint ck_exchange_rate_positive check (usd_rate > 0)
);

-- Source: European Central Bank euro foreign exchange reference rates for 2026-10-14
-- (USD 1.1195, GBP 0.84385, INR 106.5283, CAD 1.5918, AUD 1.5971, SGD 1.4359 per EUR),
-- converted to USD per unit as (USD per EUR) / (currency per EUR), rounded half-up to 8 places.
insert into exchange_rate (currency, usd_rate, effective_date) values
    ('USD', 1.00000000, date '2026-10-14'),
    ('EUR', 1.11950000, date '2026-10-14'),
    ('GBP', 1.32665758, date '2026-10-14'),
    ('INR', 0.01050894, date '2026-10-14'),
    ('CAD', 0.70329187, date '2026-10-14'),
    ('AUD', 0.70095799, date '2026-10-14'),
    ('SGD', 0.77965039, date '2026-10-14');

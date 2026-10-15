package com.acme.salary.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExchangeRateRepository {

	private final JdbcTemplate jdbc;

	public ExchangeRateRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** All configured rates. If rows carry different dates, the oldest is reported, to stay conservative. */
	public ExchangeRates currentRates() {
		Map<Currency, BigDecimal> rates = new HashMap<>();
		LocalDate[] oldest = new LocalDate[1];
		this.jdbc.query("select currency, usd_rate, effective_date from exchange_rate", (row) -> {
			rates.put(Currency.getInstance(row.getString("currency")), row.getBigDecimal("usd_rate"));
			LocalDate date = row.getObject("effective_date", LocalDate.class);
			if (oldest[0] == null || date.isBefore(oldest[0])) {
				oldest[0] = date;
			}
		});
		if (rates.isEmpty()) {
			throw new IllegalStateException("No exchange rates configured");
		}
		return new ExchangeRates(rates, oldest[0]);
	}

}

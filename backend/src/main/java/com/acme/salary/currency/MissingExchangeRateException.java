package com.acme.salary.currency;

import java.util.Currency;

/**
 * No exchange rate is configured for a currency. Converting anyway (or skipping those salaries) would give a
 * wrong total, so conversion fails instead; add the rate in a new Flyway migration.
 */
public class MissingExchangeRateException extends RuntimeException {

	MissingExchangeRateException(Currency currency) {
		super("No USD exchange rate for " + currency.getCurrencyCode());
	}

}

package com.acme.salary.employee;

import java.util.Currency;

/**
 * Countries ACME employs people in, by ISO 3166-1 alpha-2 code. Salary currency is derived from the
 * country, so it never has to be entered by hand.
 */
public enum Country {

	US("USD"),
	GB("GBP"),
	IN("INR"),
	DE("EUR"),
	FR("EUR"),
	CA("CAD"),
	AU("AUD"),
	SG("SGD");

	private final Currency currency;

	Country(String currencyCode) {
		this.currency = Currency.getInstance(currencyCode);
	}

	public Currency currency() {
		return this.currency;
	}

}

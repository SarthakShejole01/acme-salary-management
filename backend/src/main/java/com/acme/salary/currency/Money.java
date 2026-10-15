package com.acme.salary.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * A non-negative amount in a single currency, held at two decimal places.
 * Amounts with more precision are rejected rather than rounded, so a value is never silently changed.
 */
public record Money(BigDecimal amount, Currency currency) {

	private static final int SCALE = 2;

	public Money {
		if (amount == null) {
			throw new IllegalArgumentException("Money amount must not be null");
		}
		if (currency == null) {
			throw new IllegalArgumentException("Money currency must not be null");
		}
		if (amount.signum() < 0) {
			throw new IllegalArgumentException("Money amount must not be negative: " + amount);
		}
		try {
			amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
		}
		catch (ArithmeticException ex) {
			throw new IllegalArgumentException(
					"Money amount must have at most " + SCALE + " decimal places: " + amount, ex);
		}
	}

}

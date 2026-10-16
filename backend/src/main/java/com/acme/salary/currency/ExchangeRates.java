package com.acme.salary.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;

/**
 * Fixed USD conversion rates valid on one date: {@code 1 unit of currency = rate USD}. Converted figures are
 * approximate by nature; callers should say so and show {@link #asOf()}.
 */
public final class ExchangeRates {

	public static final Currency USD = Currency.getInstance("USD");

	private final Map<Currency, BigDecimal> usdRates;

	private final LocalDate asOf;

	public ExchangeRates(Map<Currency, BigDecimal> usdRates, LocalDate asOf) {
		this.usdRates = Map.copyOf(usdRates);
		this.asOf = asOf;
	}

	public LocalDate asOf() {
		return this.asOf;
	}

	/** Exact, unrounded USD value, so several amounts can be summed before rounding once. */
	public BigDecimal usdValue(Money money) {
		BigDecimal rate = this.usdRates.get(money.currency());
		if (rate == null) {
			throw new MissingExchangeRateException(money.currency());
		}
		return money.amount().multiply(rate);
	}

	/** USD value rounded half-up to cents. */
	public Money toUsd(Money money) {
		return new Money(usdValue(money).setScale(2, RoundingMode.HALF_UP), USD);
	}

}

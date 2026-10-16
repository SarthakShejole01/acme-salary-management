package com.acme.salary.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExchangeRatesTest {

	private static final Currency USD = Currency.getInstance("USD");

	private static final Currency GBP = Currency.getInstance("GBP");

	private static final Currency INR = Currency.getInstance("INR");

	private static final LocalDate RATE_DATE = LocalDate.of(2026, 10, 14);

	private final ExchangeRates rates = new ExchangeRates(
			Map.of(USD, new BigDecimal("1.00000000"), GBP, new BigDecimal("1.32665758"), INR,
					new BigDecimal("0.01050894")),
			RATE_DATE);

	@Test
	void converts_an_amount_to_usd_at_its_fixed_rate() {
		Money usd = this.rates.toUsd(money("2000000", INR));

		assertThat(usd).isEqualTo(money("21017.88", USD));
	}

	@Test
	void rounds_the_converted_amount_half_up_to_cents() {
		Money usd = this.rates.toUsd(money("100", GBP));

		assertThat(usd).isEqualTo(money("132.67", USD));
	}

	@Test
	void gives_the_exact_usd_value_so_totals_can_be_rounded_once() {
		BigDecimal exact = this.rates.usdValue(money("100", GBP));

		assertThat(exact).isEqualByComparingTo("132.665758");
	}

	@Test
	void leaves_usd_amounts_unchanged() {
		assertThat(this.rates.toUsd(money("98000", USD))).isEqualTo(money("98000", USD));
	}

	@Test
	void refuses_to_convert_a_currency_without_a_rate() {
		assertThatThrownBy(() -> this.rates.toUsd(money("100", Currency.getInstance("JPY"))))
			.isInstanceOf(MissingExchangeRateException.class)
			.hasMessageContaining("JPY");
	}

	@Test
	void reports_the_date_the_rates_apply_to() {
		assertThat(this.rates.asOf()).isEqualTo(RATE_DATE);
	}

	private static Money money(String amount, Currency currency) {
		return new Money(new BigDecimal(amount), currency);
	}

}

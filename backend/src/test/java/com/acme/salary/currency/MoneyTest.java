package com.acme.salary.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class MoneyTest {

	private static final Currency USD = Currency.getInstance("USD");
	private static final Currency EUR = Currency.getInstance("EUR");

	@Test
	void holds_amount_and_currency() {
		Money money = new Money(new BigDecimal("85000.50"), USD);

		assertThat(money.amount()).isEqualByComparingTo("85000.50");
		assertThat(money.currency()).isEqualTo(USD);
	}

	@Test
	void normalises_amount_to_two_decimal_places() {
		Money money = new Money(new BigDecimal("100"), USD);

		assertThat(money.amount().scale()).isEqualTo(2);
		assertThat(money.amount()).hasToString("100.00");
	}

	@Test
	void accepts_trailing_zeros_beyond_two_decimal_places() {
		Money money = new Money(new BigDecimal("10.5000"), USD);

		assertThat(money.amount()).hasToString("10.50");
	}

	@Test
	void rejects_amount_with_more_than_two_significant_decimal_places() {
		assertThatThrownBy(() -> new Money(new BigDecimal("10.001"), USD))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("decimal places");
	}

	@Test
	void accepts_zero_amount() {
		Money money = new Money(BigDecimal.ZERO, USD);

		assertThat(money.amount()).isEqualByComparingTo("0");
	}

	@Test
	void rejects_negative_amount() {
		assertThatThrownBy(() -> new Money(new BigDecimal("-0.01"), USD))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("negative");
	}

	@Test
	void rejects_null_amount() {
		assertThatThrownBy(() -> new Money(null, USD))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("amount");
	}

	@Test
	void rejects_null_currency() {
		assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("currency");
	}

	@Test
	void equal_when_amount_and_currency_match_regardless_of_scale() {
		Money plain = new Money(new BigDecimal("100"), USD);
		Money scaled = new Money(new BigDecimal("100.00"), USD);

		assertThat(plain).isEqualTo(scaled);
		assertThat(plain).hasSameHashCodeAs(scaled);
	}

	@Test
	void not_equal_when_currency_differs() {
		Money dollars = new Money(new BigDecimal("100"), USD);
		Money euros = new Money(new BigDecimal("100"), EUR);

		assertThat(dollars).isNotEqualTo(euros);
	}
}

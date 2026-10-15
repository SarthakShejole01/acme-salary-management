package com.acme.salary.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.salary.TestcontainersConfiguration;
import com.acme.salary.employee.Country;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** The rates come from Flyway V2, so these tests check the migration's data as well as the mapping. */
@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, ExchangeRateRepository.class })
class ExchangeRateRepositoryTest {

	@Autowired
	private ExchangeRateRepository repository;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void has_a_positive_rate_for_the_currency_of_every_supported_country() {
		ExchangeRates rates = this.repository.currentRates();

		for (Country country : Country.values()) {
			assertThat(rates.usdValue(new Money(BigDecimal.ONE, country.currency()))).as(country.name()).isPositive();
		}
	}

	@Test
	void loads_the_ecb_reference_rates_of_14_october_2026() {
		ExchangeRates rates = this.repository.currentRates();

		assertThat(rates.asOf()).isEqualTo(LocalDate.of(2026, 10, 14));
		assertThat(rates.usdValue(new Money(BigDecimal.ONE, Currency.getInstance("GBP"))))
			.isEqualByComparingTo("1.32665758");
		assertThat(rates.usdValue(new Money(BigDecimal.ONE, Currency.getInstance("USD")))).isEqualByComparingTo("1");
	}

	@Test
	void rejects_a_rate_that_is_not_positive() {
		assertThatThrownBy(() -> this.jdbc
			.update("insert into exchange_rate (currency, usd_rate, effective_date) values ('JPY', 0, date '2026-10-14')"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_exchange_rate_positive");
	}

}

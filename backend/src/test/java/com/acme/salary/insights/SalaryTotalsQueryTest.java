package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.TestcontainersConfiguration;
import com.acme.salary.currency.Money;
import com.acme.salary.employee.Country;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, InsightsQueries.class })
class SalaryTotalsQueryTest {

	@Autowired
	private InsightsQueries queries;

	@Autowired
	private JdbcTemplate jdbc;

	private EmployeeRows rows;

	@BeforeEach
	void setUp() {
		this.rows = new EmployeeRows(this.jdbc);
	}

	@Test
	void sums_salaries_per_currency_without_mixing_them() {
		this.rows.insert(Country.DE, "Accountant", "50000", "60000");
		this.rows.insert(Country.FR, "Accountant", "40000");
		this.rows.insert(Country.IN, "Accountant", "1200000");

		assertThat(this.queries.salaryTotalsByCurrency()).containsExactlyInAnyOrder(
				new CurrencyTotal(3, money("150000", "EUR")), new CurrencyTotal(1, money("1200000", "INR")));
	}

	@Test
	void has_no_totals_when_there_are_no_employees() {
		assertThat(this.queries.salaryTotalsByCurrency()).isEmpty();
	}

	private static Money money(String amount, String currency) {
		return new Money(new BigDecimal(amount), Currency.getInstance(currency));
	}

}

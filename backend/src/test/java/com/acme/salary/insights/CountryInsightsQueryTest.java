package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.acme.salary.TestcontainersConfiguration;
import com.acme.salary.currency.Money;
import com.acme.salary.employee.Country;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Aggregations run in PostgreSQL; small known data sets make the expected numbers easy to check by hand. */
@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, InsightsQueries.class })
class CountryInsightsQueryTest {

	private static final Currency INR = Currency.getInstance("INR");

	private static final Currency USD = Currency.getInstance("USD");

	@Autowired
	private InsightsQueries queries;

	@Autowired
	private JdbcTemplate jdbc;

	private int sequence;

	@Test
	void reports_headcount_min_and_max_per_country() {
		insert(Country.IN, "100", "1000", "300");
		insert(Country.US, "45", "10");

		List<CountryInsight> insights = this.queries.countryInsights();

		assertThat(insights).extracting(CountryInsight::countryCode, CountryInsight::headcount,
				CountryInsight::min, CountryInsight::max)
			.containsExactlyInAnyOrder(
					tuple("IN", 3L, inr("100"), inr("1000")),
					tuple("US", 2L, usd("10"), usd("45")));
	}

	@Test
	void average_is_the_mean_in_local_currency() {
		insert(Country.US, "10", "20", "30", "45");

		CountryInsight us = only(this.queries.countryInsights());

		assertThat(us.average()).isEqualTo(usd("26.25"));
	}

	@Test
	void median_of_odd_count_is_the_middle_value() {
		insert(Country.IN, "100", "1000", "300", "200", "400");

		CountryInsight in = only(this.queries.countryInsights());

		assertThat(in.median()).isEqualTo(inr("300"));
	}

	@Test
	void median_of_even_count_averages_middle_values() {
		insert(Country.US, "10", "45", "20", "30");

		CountryInsight us = only(this.queries.countryInsights());

		assertThat(us.median()).isEqualTo(usd("25"));
	}

	@Test
	void average_and_median_are_rounded_half_up_to_cents() {
		insert(Country.US, "10.01", "10.02");

		CountryInsight us = only(this.queries.countryInsights());

		assertThat(us.average()).isEqualTo(usd("10.02"));
		assertThat(us.median()).isEqualTo(usd("10.02"));
	}

	@Test
	void never_mixes_currencies_between_countries() {
		insert(Country.IN, "2000000");
		insert(Country.US, "100000");

		List<CountryInsight> insights = this.queries.countryInsights();

		assertThat(insights).allSatisfy((insight) -> assertThat(insight.average().currency())
			.isEqualTo(Country.valueOf(insight.countryCode()).currency()));
	}

	@Test
	void omits_countries_without_employees() {
		insert(Country.IN, "100");

		assertThat(this.queries.countryInsights()).extracting(CountryInsight::countryCode).containsExactly("IN");
	}

	private void insert(Country country, String... salaries) {
		for (String salary : salaries) {
			this.sequence++;
			this.jdbc.update("""
					insert into employee (employee_code, full_name, email, job_title, department, country_code,
					                      currency, salary, employment_type, hire_date)
					values (?, 'Test Person', ?, 'Software Engineer', 'Engineering', ?, ?, ?, 'FULL_TIME',
					        date '2024-01-01')
					""", "T-" + this.sequence, "t" + this.sequence + "@acme.com", country.name(),
					country.currency().getCurrencyCode(), new BigDecimal(salary));
		}
	}

	private static CountryInsight only(List<CountryInsight> insights) {
		assertThat(insights).hasSize(1);
		return insights.getFirst();
	}

	private static Money inr(String amount) {
		return new Money(new BigDecimal(amount), INR);
	}

	private static Money usd(String amount) {
		return new Money(new BigDecimal(amount), USD);
	}

}

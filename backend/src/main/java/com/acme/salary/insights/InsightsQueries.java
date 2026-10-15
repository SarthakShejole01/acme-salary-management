package com.acme.salary.insights;

import com.acme.salary.currency.Money;
import com.acme.salary.employee.Country;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only salary aggregations, computed in PostgreSQL. Every query groups by currency as well as country,
 * so amounts in different currencies are never combined.
 */
@Repository
class InsightsQueries {

	/**
	 * The median is exact {@code numeric}: the mean of the lower and upper middle values, found with
	 * {@code percentile_disc} in ascending and descending order ({@code percentile_cont} would go through
	 * {@code double precision}). Average and median are rounded half-up to cents.
	 */
	private static final String COUNTRY_INSIGHTS = """
			select country_code,
			       currency,
			       count(*)                                                        as headcount,
			       min(salary)                                                     as min_salary,
			       max(salary)                                                     as max_salary,
			       round(avg(salary), 2)                                           as avg_salary,
			       round((percentile_disc(0.5) within group (order by salary)
			            + percentile_disc(0.5) within group (order by salary desc)) / 2, 2) as median_salary
			from employee
			group by country_code, currency
			""";

	private final JdbcTemplate jdbc;

	InsightsQueries(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	List<CountryInsight> countryInsights() {
		return this.jdbc.query(COUNTRY_INSIGHTS, InsightsQueries::countryInsight);
	}

	private static CountryInsight countryInsight(ResultSet row, int rowNumber) throws SQLException {
		Country country = Country.valueOf(row.getString("country_code"));
		Currency currency = Currency.getInstance(row.getString("currency"));
		return new CountryInsight(country.name(), country.displayName(), row.getLong("headcount"),
				new Money(row.getBigDecimal("min_salary"), currency), new Money(row.getBigDecimal("max_salary"), currency),
				new Money(row.getBigDecimal("avg_salary"), currency),
				new Money(row.getBigDecimal("median_salary"), currency));
	}

}

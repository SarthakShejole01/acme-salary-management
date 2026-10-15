package com.acme.salary.insights;

import com.acme.salary.currency.Money;
import com.acme.salary.employee.Country;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only salary aggregations, computed in PostgreSQL. Every query groups by currency as well as its other
 * keys, so amounts in different currencies are never combined.
 */
@Repository
class InsightsQueries {

	/**
	 * Statistics shared by all groupings. The median is exact {@code numeric}: the mean of the lower and upper
	 * middle values, found with {@code percentile_disc} in ascending and descending order ({@code percentile_cont}
	 * would go through {@code double precision}). Average and median are rounded half-up to cents.
	 */
	private static final String SALARY_STATISTICS = """
			       currency,
			       count(*)                                                        as headcount,
			       min(salary)                                                     as min_salary,
			       max(salary)                                                     as max_salary,
			       round(avg(salary), 2)                                           as avg_salary,
			       round((percentile_disc(0.5) within group (order by salary)
			            + percentile_disc(0.5) within group (order by salary desc)) / 2, 2) as median_salary
			""";

	private static final String COUNTRY_INSIGHTS = "select country_code," + SALARY_STATISTICS + """
			from employee
			group by country_code, currency
			""";

	private static final String JOB_TITLE_INSIGHTS = "select job_title," + SALARY_STATISTICS + """
			from employee
			where country_code = ?
			group by job_title, currency
			order by job_title
			""";

	private static final String SALARY_RANGE = """
			select min(salary) as min_salary, max(salary) as max_salary, currency
			from employee
			where country_code = ?
			group by currency
			""";

	private static final String BUCKET_COUNTS = """
			select width_bucket(salary, ?, ?, ?) as bucket, count(*) as headcount
			from employee
			where country_code = ?
			group by bucket
			""";

	private final JdbcTemplate jdbc;

	InsightsQueries(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	List<CountryInsight> countryInsights() {
		return this.jdbc.query(COUNTRY_INSIGHTS, (row, rowNumber) -> {
			Country country = Country.valueOf(row.getString("country_code"));
			return new CountryInsight(country.name(), country.displayName(), row.getLong("headcount"),
					money(row, "min_salary"), money(row, "max_salary"), money(row, "avg_salary"),
					money(row, "median_salary"));
		});
	}

	List<JobTitleInsight> jobTitleInsights(Country country) {
		return this.jdbc.query(JOB_TITLE_INSIGHTS,
				(row, rowNumber) -> new JobTitleInsight(row.getString("job_title"), row.getLong("headcount"),
						money(row, "min_salary"), money(row, "max_salary"), money(row, "avg_salary"),
						money(row, "median_salary")),
				country.name());
	}

	Optional<SalaryRange> salaryRange(Country country) {
		return this.jdbc
			.query(SALARY_RANGE,
					(row, rowNumber) -> new SalaryRange(row.getBigDecimal("min_salary"), row.getBigDecimal("max_salary"),
							Currency.getInstance(row.getString("currency"))),
					country.name())
			.stream()
			.findFirst();
	}

	/** Headcount per bucket number (1-based); buckets nobody falls into are absent. */
	Map<Integer, Long> bucketCounts(Country country, HistogramBins bins) {
		Map<Integer, Long> counts = new HashMap<>();
		this.jdbc.query(BUCKET_COUNTS,
				(row) -> {
					counts.put(row.getInt("bucket"), row.getLong("headcount"));
				},
				bins.start(), bins.end(), bins.count(), country.name());
		return counts;
	}

	private static Money money(ResultSet row, String column) throws SQLException {
		return new Money(row.getBigDecimal(column), Currency.getInstance(row.getString("currency")));
	}

}

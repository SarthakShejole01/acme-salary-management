package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.TestcontainersConfiguration;
import com.acme.salary.employee.Country;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;
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
class DistributionQueryTest {

	private static final HistogramBins TEN_THOUSAND_WIDE = new HistogramBins(new BigDecimal("10000"),
			new BigDecimal("10000"), 3);

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
	void finds_the_salary_range_and_currency_of_a_country() {
		this.rows.insert(Country.GB, "Accountant", "52000", "16000", "109500");

		SalaryRange range = this.queries.salaryRange(Country.GB).orElseThrow();

		assertThat(range.min()).isEqualByComparingTo("16000");
		assertThat(range.max()).isEqualByComparingTo("109500");
		assertThat(range.currency()).isEqualTo(Currency.getInstance("GBP"));
	}

	@Test
	void has_no_salary_range_for_a_country_without_employees() {
		this.rows.insert(Country.GB, "Accountant", "52000");

		assertThat(this.queries.salaryRange(Country.SG)).isEmpty();
	}

	@Test
	void counts_employees_per_bucket_including_the_lower_edge() {
		this.rows.insert(Country.GB, "Accountant", "10000", "19999.99", "20000", "35000");

		Map<Integer, Long> counts = this.queries.bucketCounts(Country.GB, TEN_THOUSAND_WIDE);

		assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(1, 2L, 2, 1L, 3, 1L));
	}

	@Test
	void counts_only_employees_of_the_requested_country() {
		this.rows.insert(Country.GB, "Accountant", "15000");
		this.rows.insert(Country.US, "Accountant", "15000");

		Map<Integer, Long> counts = this.queries.bucketCounts(Country.GB, TEN_THOUSAND_WIDE);

		assertThat(counts).containsExactly(Map.entry(1, 1L));
	}

}

package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.TestcontainersConfiguration;
import com.acme.salary.currency.Money;
import com.acme.salary.employee.Country;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
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
class JobTitleInsightsQueryTest {

	private static final Currency INR = Currency.getInstance("INR");

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
	void reports_statistics_for_each_job_title_in_the_country() {
		this.rows.insert(Country.IN, "Software Engineer", "1000000", "3000000", "1500000", "2000000");

		JobTitleInsight engineers = only(this.queries.jobTitleInsights(Country.IN));

		assertThat(engineers).isEqualTo(new JobTitleInsight("Software Engineer", 4, inr("1000000"), inr("3000000"),
				inr("1875000"), inr("1750000")));
	}

	@Test
	void ignores_employees_in_other_countries() {
		this.rows.insert(Country.IN, "Software Engineer", "2000000");
		this.rows.insert(Country.US, "Software Engineer", "120000");

		JobTitleInsight engineers = only(this.queries.jobTitleInsights(Country.IN));

		assertThat(engineers.headcount()).isEqualTo(1);
		assertThat(engineers.max()).isEqualTo(inr("2000000"));
	}

	@Test
	void lists_job_titles_alphabetically() {
		this.rows.insert(Country.IN, "Software Engineer", "2000000");
		this.rows.insert(Country.IN, "Accountant", "900000");
		this.rows.insert(Country.IN, "Data Analyst", "1200000");

		List<JobTitleInsight> insights = this.queries.jobTitleInsights(Country.IN);

		assertThat(insights).extracting(JobTitleInsight::jobTitle)
			.containsExactly("Accountant", "Data Analyst", "Software Engineer");
	}

	@Test
	void returns_nothing_for_a_country_without_employees() {
		this.rows.insert(Country.US, "Software Engineer", "120000");

		assertThat(this.queries.jobTitleInsights(Country.SG)).isEmpty();
	}

	private static JobTitleInsight only(List<JobTitleInsight> insights) {
		assertThat(insights).hasSize(1);
		return insights.getFirst();
	}

	private static Money inr(String amount) {
		return new Money(new BigDecimal(amount), INR);
	}

}

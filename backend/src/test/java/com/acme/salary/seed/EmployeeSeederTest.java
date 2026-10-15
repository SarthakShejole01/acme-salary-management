package com.acme.salary.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.TestcontainersConfiguration;
import com.acme.salary.employee.Country;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, EmployeeSeeder.class, EmployeeSeederTest.FixedClock.class })
class EmployeeSeederTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);

	@Autowired
	private EmployeeSeeder seeder;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void seeds_ten_thousand_employees_into_an_empty_database() {
		SeedResult result = this.seeder.seed(false);

		assertThat(result).isEqualTo(new SeedResult(SeedResult.Outcome.SEEDED, 10_000));
		assertThat(employeeCount()).isEqualTo(10_000);
	}

	@Test
	void stores_exactly_what_the_generator_produces() {
		this.seeder.seed(false);

		Map<String, Object> first = this.jdbc.queryForMap(
				"select full_name, email, country_code, currency, salary, hire_date from employee where employee_code = 'EMP-00001'");
		var expected = new EmployeeGenerator(NameLists.fromClasspath(), TODAY).generate(1, EmployeeSeeder.RANDOM_SEED)
			.getFirst();

		assertThat(first).containsEntry("full_name", expected.fullName())
			.containsEntry("email", expected.email())
			.containsEntry("country_code", expected.country().name())
			.containsEntry("currency", expected.country().currency().getCurrencyCode())
			.containsEntry("salary", expected.salary())
			.containsEntry("hire_date", java.sql.Date.valueOf(expected.hireDate()));
	}

	@Test
	void stores_every_salary_in_the_currency_of_its_country() {
		this.seeder.seed(false);

		List<Map<String, Object>> pairs = this.jdbc
			.queryForList("select distinct country_code, currency from employee order by country_code");

		assertThat(pairs).hasSize(Country.values().length)
			.allSatisfy((pair) -> assertThat(pair.get("currency"))
				.isEqualTo(Country.valueOf((String) pair.get("country_code")).currency().getCurrencyCode()));
	}

	@Test
	void skips_seeding_when_employees_already_exist() {
		insertOneEmployee();

		SeedResult result = this.seeder.seed(false);

		assertThat(result).isEqualTo(new SeedResult(SeedResult.Outcome.SKIPPED, 1));
		assertThat(employeeCount()).isEqualTo(1);
	}

	@Test
	void reset_replaces_existing_employees_with_fresh_seed_data() {
		insertOneEmployee();

		SeedResult result = this.seeder.seed(true);

		assertThat(result).isEqualTo(new SeedResult(SeedResult.Outcome.SEEDED, 10_000));
		assertThat(this.jdbc.queryForObject("select count(*) from employee where employee_code = 'MANUAL-1'",
				Long.class)).isZero();
	}

	private long employeeCount() {
		return this.jdbc.queryForObject("select count(*) from employee", Long.class);
	}

	private void insertOneEmployee() {
		this.jdbc.update("""
				insert into employee (employee_code, full_name, email, job_title, department, country_code, currency,
				                      salary, employment_type, hire_date)
				values ('MANUAL-1', 'Manual Entry', 'manual@acme.com', 'Accountant', 'Finance', 'GB', 'GBP',
				        40000, 'FULL_TIME', date '2024-01-01')
				""");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClock {

		@Bean
		Clock clock() {
			return Clock.fixed(Instant.parse("2026-01-15T09:30:00Z"), ZoneOffset.UTC);
		}

	}

}

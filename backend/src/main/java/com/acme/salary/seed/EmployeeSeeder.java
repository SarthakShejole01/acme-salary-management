package com.acme.salary.seed;

import com.acme.salary.employee.EmployeeRequest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the generated employees with JDBC batch inserts (bypassing JPA, which would be far slower for 10k rows).
 * Idempotent: an existing data set is left alone unless a reset is requested.
 */
@Component
public class EmployeeSeeder {

	static final long RANDOM_SEED = 42L;

	static final int EMPLOYEE_COUNT = 10_000;

	static final int BATCH_SIZE = 1_000;

	private static final String INSERT = """
			insert into employee (employee_code, full_name, email, job_title, department, country_code, currency,
			                      salary, employment_type, hire_date)
			values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";

	private final JdbcTemplate jdbc;

	private final Clock clock;

	public EmployeeSeeder(JdbcTemplate jdbc, Clock clock) {
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Transactional
	public SeedResult seed(boolean reset) {
		if (reset) {
			this.jdbc.execute("truncate table employee restart identity");
		}
		long existing = this.jdbc.queryForObject("select count(*) from employee", Long.class);
		if (existing > 0) {
			return new SeedResult(SeedResult.Outcome.SKIPPED, existing);
		}
		List<EmployeeRequest> employees = new EmployeeGenerator(NameLists.fromClasspath(), LocalDate.now(this.clock))
			.generate(EMPLOYEE_COUNT, RANDOM_SEED);
		this.jdbc.batchUpdate(INSERT, employees, BATCH_SIZE, (statement, employee) -> {
			statement.setString(1, employee.employeeCode());
			statement.setString(2, employee.fullName());
			statement.setString(3, employee.email());
			statement.setString(4, employee.jobTitle());
			statement.setString(5, employee.department());
			statement.setString(6, employee.country().name());
			statement.setString(7, employee.country().currency().getCurrencyCode());
			statement.setBigDecimal(8, employee.salary());
			statement.setString(9, employee.employmentType().name());
			statement.setObject(10, employee.hireDate());
		});
		return new SeedResult(SeedResult.Outcome.SEEDED, employees.size());
	}

}

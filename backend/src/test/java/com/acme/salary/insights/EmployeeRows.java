package com.acme.salary.insights;

import com.acme.salary.employee.Country;
import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;

/** Inserts minimal employee rows for aggregation tests; only country, job title and salary vary. */
class EmployeeRows {

	private final JdbcTemplate jdbc;

	private int sequence;

	EmployeeRows(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	void insert(Country country, String jobTitle, String... salaries) {
		for (String salary : salaries) {
			this.sequence++;
			this.jdbc.update("""
					insert into employee (employee_code, full_name, email, job_title, department, country_code,
					                      currency, salary, employment_type, hire_date)
					values (?, 'Test Person', ?, ?, 'Engineering', ?, ?, ?, 'FULL_TIME', date '2024-01-01')
					""", "T-" + this.sequence, "t" + this.sequence + "@acme.com", jobTitle, country.name(),
					country.currency().getCurrencyCode(), new BigDecimal(salary));
		}
	}

}

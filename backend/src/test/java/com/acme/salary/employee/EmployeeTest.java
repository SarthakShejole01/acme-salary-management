package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.currency.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import org.junit.jupiter.api.Test;

class EmployeeTest {

	@Test
	void salary_is_held_in_the_currency_of_the_employees_country() {
		Employee employee = new Employee("EMP-00001", "Priya Sharma", "priya.sharma@acme.com",
				"Software Engineer", "Engineering", Country.IN, new BigDecimal("2400000"),
				EmploymentType.FULL_TIME, LocalDate.of(2022, 4, 1));

		Money salary = employee.getSalary();

		assertThat(salary).isEqualTo(new Money(new BigDecimal("2400000.00"), Currency.getInstance("INR")));
	}

}

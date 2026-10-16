package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.currency.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EmployeeCsvTest {

	@Test
	void starts_with_a_header_naming_every_column() {
		assertThat(EmployeeCsv.header()).isEqualTo("Employee code,Full name,Email,Job title,Department,Country,"
				+ "Currency,Annual salary,Employment type,Hire date\r\n");
	}

	@Test
	void writes_one_line_per_employee_with_plain_salary_and_iso_date() {
		String row = EmployeeCsv.row(employee("Priya Sharma", "Software Engineer"));

		assertThat(row).isEqualTo("EMP-00001,Priya Sharma,priya.sharma@acme.com,Software Engineer,Engineering,IN,"
				+ "INR,2400000.00,FULL_TIME,2022-04-01\r\n");
	}

	@Test
	void quotes_values_containing_commas_quotes_or_line_breaks() {
		String row = EmployeeCsv.row(employee("Sharma, Priya \"PS\"", "Engineer\nLead"));

		assertThat(row).contains(",\"Sharma, Priya \"\"PS\"\"\",").contains(",\"Engineer\nLead\",");
	}

	@ParameterizedTest
	@ValueSource(strings = { "=HYPERLINK(\"http://x\")", "+1+1", "-2+3", "@SUM(A1)", "\tTab", "\rReturn" })
	void neutralises_values_a_spreadsheet_would_run_as_formulas(String value) {
		String row = EmployeeCsv.row(employee(value, "Software Engineer"));

		// Prefixed with an apostrophe, inside quotes when the value also needs quoting.
		assertThat(row).containsAnyOf(",'" + value.charAt(0), ",\"'" + value.charAt(0));
	}

	@Test
	void leaves_ordinary_hyphenated_text_alone() {
		String row = EmployeeCsv.row(employee("Priya Sharma-Rao", "Software Engineer"));

		assertThat(row).contains(",Priya Sharma-Rao,");
	}

	private static EmployeeResponse employee(String fullName, String jobTitle) {
		return new EmployeeResponse(1L, "EMP-00001", fullName, "priya.sharma@acme.com", jobTitle, "Engineering",
				Country.IN, new Money(new BigDecimal("2400000"), Currency.getInstance("INR")), EmploymentType.FULL_TIME,
				LocalDate.of(2022, 4, 1));
	}

}

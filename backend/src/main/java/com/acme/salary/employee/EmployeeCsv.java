package com.acme.salary.employee;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Formats employees as CSV (RFC 4180: comma separated, CRLF line ends, fields with commas, quotes or line
 * breaks quoted). Text that a spreadsheet would run as a formula is prefixed with an apostrophe (OWASP CSV
 * injection guidance), because HR opens these files in Excel.
 */
final class EmployeeCsv {

	private static final String LINE_END = "\r\n";

	private static final String FORMULA_TRIGGERS = "=+-@\t\r";

	private EmployeeCsv() {
	}

	static String header() {
		return line(List.of("Employee code", "Full name", "Email", "Job title", "Department", "Country", "Currency",
				"Annual salary", "Employment type", "Hire date"));
	}

	static String row(EmployeeResponse employee) {
		return line(List.of(text(employee.employeeCode()), text(employee.fullName()), text(employee.email()),
				text(employee.jobTitle()), text(employee.department()), employee.country().name(),
				employee.salary().currency().getCurrencyCode(), employee.salary().amount().toPlainString(),
				employee.employmentType().name(), employee.hireDate().toString()));
	}

	private static String line(List<String> fields) {
		return fields.stream().map(EmployeeCsv::quoteIfNeeded).collect(Collectors.joining(",")) + LINE_END;
	}

	/** Free text typed by a user: neutralise a leading formula character. */
	private static String text(String value) {
		return (!value.isEmpty() && FORMULA_TRIGGERS.indexOf(value.charAt(0)) >= 0) ? "'" + value : value;
	}

	private static String quoteIfNeeded(String value) {
		if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

}

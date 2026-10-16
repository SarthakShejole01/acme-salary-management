package com.acme.salary.employee;

/**
 * Optional filters for the employee list; {@code null} or blank means "any". {@code search} is a
 * case-insensitive substring of name, employee code or email; the others are exact matches.
 */
public record EmployeeSearchCriteria(String search, Country country, String department, String jobTitle) {

	public EmployeeSearchCriteria {
		search = blankToNull(search);
		department = blankToNull(department);
		jobTitle = blankToNull(jobTitle);
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}

}

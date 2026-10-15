package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Data needed to create or update an employee. Salary currency is not supplied: it follows the country. */
public record EmployeeRequest(String employeeCode, String fullName, String email, String jobTitle,
		String department, Country country, BigDecimal salary, EmploymentType employmentType,
		LocalDate hireDate) {

}

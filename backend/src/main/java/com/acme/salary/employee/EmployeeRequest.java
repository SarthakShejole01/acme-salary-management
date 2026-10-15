package com.acme.salary.employee;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data needed to create or update an employee. Salary currency is not supplied: it follows the country.
 * Sizes match the database columns. "Hire date not in the future" is checked by the service, which owns
 * the clock.
 */
public record EmployeeRequest(
		@NotBlank @Size(max = 20) String employeeCode,
		@NotBlank @Size(max = 200) String fullName,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(max = 100) String jobTitle,
		@NotBlank @Size(max = 100) String department,
		@NotNull Country country,
		@NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal salary,
		@NotNull EmploymentType employmentType,
		@NotNull LocalDate hireDate) {

}

package com.acme.salary.employee;

import com.acme.salary.currency.Money;
import java.time.LocalDate;

public record EmployeeResponse(Long id, String employeeCode, String fullName, String email, String jobTitle,
		String department, Country country, Money salary, EmploymentType employmentType, LocalDate hireDate) {

	static EmployeeResponse from(Employee employee) {
		return new EmployeeResponse(employee.getId(), employee.getEmployeeCode(), employee.getFullName(),
				employee.getEmail(), employee.getJobTitle(), employee.getDepartment(), employee.getCountry(),
				employee.getSalary(), employee.getEmploymentType(), employee.getHireDate());
	}

}

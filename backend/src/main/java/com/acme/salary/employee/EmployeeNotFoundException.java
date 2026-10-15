package com.acme.salary.employee;

public class EmployeeNotFoundException extends RuntimeException {

	EmployeeNotFoundException(Long id) {
		super("Employee " + id + " does not exist");
	}

}

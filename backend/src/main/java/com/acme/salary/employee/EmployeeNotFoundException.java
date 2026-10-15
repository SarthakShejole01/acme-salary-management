package com.acme.salary.employee;

import com.acme.salary.common.NotFoundException;

public class EmployeeNotFoundException extends NotFoundException {

	EmployeeNotFoundException(Long id) {
		super("Employee " + id + " does not exist");
	}

}

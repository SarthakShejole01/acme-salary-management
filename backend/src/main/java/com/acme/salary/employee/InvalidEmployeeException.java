package com.acme.salary.employee;

import com.acme.salary.common.BusinessRuleException;

/** An employee field breaks a business rule, e.g. a non-positive salary or a future hire date. */
public class InvalidEmployeeException extends BusinessRuleException {

	InvalidEmployeeException(String field, String message) {
		super(field, message);
	}

}

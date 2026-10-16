package com.acme.salary.employee;

import com.acme.salary.common.ConflictException;

/** Another employee already uses this value, e.g. the same employee code or email. */
public class DuplicateEmployeeException extends ConflictException {

	DuplicateEmployeeException(String field, String message) {
		super(field, message);
	}

}

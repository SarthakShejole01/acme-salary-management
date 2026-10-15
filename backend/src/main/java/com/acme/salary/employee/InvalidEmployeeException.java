package com.acme.salary.employee;

/** An employee field breaks a business rule, e.g. a non-positive salary or a future hire date. */
public class InvalidEmployeeException extends RuntimeException {

	private final String field;

	InvalidEmployeeException(String field, String message) {
		super(message);
		this.field = field;
	}

	/** Name of the offending request field. */
	public String getField() {
		return this.field;
	}

}

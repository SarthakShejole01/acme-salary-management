package com.acme.salary.employee;

/** Another employee already uses this value, e.g. the same employee code or email. */
public class DuplicateEmployeeException extends RuntimeException {

	private final String field;

	DuplicateEmployeeException(String field, String message) {
		super(message);
		this.field = field;
	}

	/** Name of the offending request field. */
	public String getField() {
		return this.field;
	}

}

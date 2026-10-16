package com.acme.salary.common;

/** Another record already uses a value that must be unique. Rendered as HTTP 409. */
public abstract class ConflictException extends RuntimeException {

	private final String field;

	protected ConflictException(String field, String message) {
		super(message);
		this.field = field;
	}

	/** Name of the offending request field. */
	public String getField() {
		return this.field;
	}

}

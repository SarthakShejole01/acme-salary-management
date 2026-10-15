package com.acme.salary.common;

/** A request field breaks a business rule. Rendered as HTTP 400. */
public abstract class BusinessRuleException extends RuntimeException {

	private final String field;

	protected BusinessRuleException(String field, String message) {
		super(message);
		this.field = field;
	}

	/** Name of the offending request field. */
	public String getField() {
		return this.field;
	}

}

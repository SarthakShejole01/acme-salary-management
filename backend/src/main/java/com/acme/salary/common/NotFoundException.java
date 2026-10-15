package com.acme.salary.common;

/** The requested resource does not exist. Rendered as HTTP 404. */
public abstract class NotFoundException extends RuntimeException {

	protected NotFoundException(String message) {
		super(message);
	}

}

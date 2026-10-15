package com.acme.salary.common;

/** One entry of the {@code errors} list in a problem response. */
public record FieldViolation(String field, String message) {

}

package com.acme.salary.common;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into RFC 7807 problem responses. Spring's base class already covers framework errors
 * (malformed JSON, unsupported method or media type, ...).
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return withFieldError(HttpStatus.CONFLICT, ex.getField(), ex.getMessage());
	}

	@ExceptionHandler(BusinessRuleException.class)
	ProblemDetail handleBusinessRule(BusinessRuleException ex) {
		return withFieldError(HttpStatus.BAD_REQUEST, ex.getField(), ex.getMessage());
	}

	/**
	 * Backstop for a unique constraint hit by a concurrent request after the service's own checks passed.
	 * The database message is not exposed.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The change conflicts with existing data. Reload and try again.");
	}

	/** Adds the failing fields to Spring's default 400 problem. */
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map((error) -> new FieldViolation(error.getField(), error.getDefaultMessage()))
			.toList();
		ProblemDetail problem = ex.getBody();
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	private static ProblemDetail withFieldError(HttpStatus status, String field, String message) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
		problem.setProperty("errors", List.of(new FieldViolation(field, message)));
		return problem;
	}

}

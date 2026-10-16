package com.acme.salary.employee;

import com.acme.salary.common.BusinessRuleException;

/** The requested sort property is not one the employee list can be ordered by. */
public class UnsupportedSortException extends BusinessRuleException {

	UnsupportedSortException(String property) {
		super("sort", "Cannot sort employees by " + property);
	}

}

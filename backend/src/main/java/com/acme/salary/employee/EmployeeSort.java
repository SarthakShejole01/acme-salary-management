package com.acme.salary.employee;

import java.util.Set;
import org.springframework.data.domain.Sort;

/** The orders the employee list and export accept. */
final class EmployeeSort {

	private static final Set<String> SORTABLE_PROPERTIES = Set.of("employeeCode", "fullName", "email", "jobTitle",
			"department", "country", "salary", "employmentType", "hireDate");

	private static final Sort DEFAULT_SORT = Sort.by("fullName");

	private EmployeeSort() {
	}

	/**
	 * The requested sort if every property is supported (name order when none is given), always followed by id
	 * as a tie-breaker so paging is stable.
	 * @throws UnsupportedSortException for any other property
	 */
	static Sort supported(Sort requested) {
		for (Sort.Order order : requested) {
			if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
				throw new UnsupportedSortException(order.getProperty());
			}
		}
		return (requested.isSorted() ? requested : DEFAULT_SORT).and(Sort.by("id"));
	}

}

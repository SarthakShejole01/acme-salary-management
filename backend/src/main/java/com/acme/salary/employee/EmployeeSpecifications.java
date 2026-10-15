package com.acme.salary.employee;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class EmployeeSpecifications {

	private static final char ESCAPE = '\\';

	private EmployeeSpecifications() {
	}

	static Specification<Employee> matching(EmployeeSearchCriteria criteria) {
		List<Specification<Employee>> specifications = new ArrayList<>();
		if (criteria.search() != null) {
			specifications.add(nameCodeOrEmailContains(criteria.search()));
		}
		if (criteria.country() != null) {
			specifications.add(equal("country", criteria.country()));
		}
		if (criteria.department() != null) {
			specifications.add(equal("department", criteria.department()));
		}
		if (criteria.jobTitle() != null) {
			specifications.add(equal("jobTitle", criteria.jobTitle()));
		}
		return Specification.allOf(specifications);
	}

	private static Specification<Employee> nameCodeOrEmailContains(String text) {
		String pattern = "%" + escapeLikeWildcards(text.toLowerCase(Locale.ROOT)) + "%";
		return (root, query, cb) -> cb.or(
				cb.like(cb.lower(root.get("fullName")), pattern, ESCAPE),
				cb.like(cb.lower(root.get("employeeCode")), pattern, ESCAPE),
				cb.like(cb.lower(root.get("email")), pattern, ESCAPE));
	}

	private static Specification<Employee> equal(String attribute, Object value) {
		return (root, query, cb) -> cb.equal(root.get(attribute), value);
	}

	/** So that "%" and "_" typed by the user match themselves rather than acting as wildcards. */
	private static String escapeLikeWildcards(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}

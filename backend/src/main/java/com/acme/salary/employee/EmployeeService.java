package com.acme.salary.employee;

import com.acme.salary.common.PageResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for maintaining employees. Uniqueness is checked here to give a clear error; the database
 * constraints remain the final guard against concurrent duplicates.
 */
@Service
@Transactional
public class EmployeeService {

	private static final Set<String> SORTABLE_PROPERTIES = Set.of("employeeCode", "fullName", "email", "jobTitle",
			"department", "country", "salary", "employmentType", "hireDate");

	private static final Sort DEFAULT_SORT = Sort.by("fullName");

	private final EmployeeRepository repository;

	private final Clock clock;

	public EmployeeService(EmployeeRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PageResponse<EmployeeResponse> search(EmployeeSearchCriteria criteria, Pageable pageable) {
		Pageable page = withSupportedSort(pageable);
		return PageResponse
			.from(this.repository.findAll(EmployeeSpecifications.matching(criteria), page).map(EmployeeResponse::from));
	}

	@Transactional(readOnly = true)
	public EmployeeResponse get(Long id) {
		return EmployeeResponse.from(findExisting(id));
	}

	public EmployeeResponse create(EmployeeRequest request) {
		validateFields(request);
		if (this.repository.existsByEmployeeCode(request.employeeCode())) {
			throw duplicateCode(request);
		}
		if (this.repository.existsByEmailIgnoreCase(request.email())) {
			throw duplicateEmail(request);
		}
		Employee employee = new Employee(request.employeeCode(), request.fullName(), request.email(),
				request.jobTitle(), request.department(), request.country(), request.salary(),
				request.employmentType(), request.hireDate());
		return EmployeeResponse.from(this.repository.save(employee));
	}

	public EmployeeResponse update(Long id, EmployeeRequest request) {
		Employee employee = findExisting(id);
		validateFields(request);
		if (this.repository.existsByEmployeeCodeAndIdNot(request.employeeCode(), id)) {
			throw duplicateCode(request);
		}
		if (this.repository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
			throw duplicateEmail(request);
		}
		employee.changeDetails(request.employeeCode(), request.fullName(), request.email(), request.jobTitle(),
				request.department(), request.country(), request.salary(), request.employmentType(),
				request.hireDate());
		return EmployeeResponse.from(employee);
	}

	public void delete(Long id) {
		this.repository.delete(findExisting(id));
	}

	/** Defaults to name order and always adds id as a tie-breaker, so paging is stable. */
	private static Pageable withSupportedSort(Pageable pageable) {
		for (Sort.Order order : pageable.getSort()) {
			if (!SORTABLE_PROPERTIES.contains(order.getProperty())) {
				throw new UnsupportedSortException(order.getProperty());
			}
		}
		Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT;
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.and(Sort.by("id")));
	}

	private Employee findExisting(Long id) {
		return this.repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
	}

	private void validateFields(EmployeeRequest request) {
		if (request.salary().signum() <= 0) {
			throw new InvalidEmployeeException("salary", "Salary must be greater than zero");
		}
		if (request.hireDate().isAfter(LocalDate.now(this.clock))) {
			throw new InvalidEmployeeException("hireDate", "Hire date must not be in the future");
		}
	}

	private static DuplicateEmployeeException duplicateCode(EmployeeRequest request) {
		return new DuplicateEmployeeException("employeeCode",
				"Employee code " + request.employeeCode() + " is already in use");
	}

	private static DuplicateEmployeeException duplicateEmail(EmployeeRequest request) {
		return new DuplicateEmployeeException("email", "Email " + request.email() + " is already in use");
	}

}

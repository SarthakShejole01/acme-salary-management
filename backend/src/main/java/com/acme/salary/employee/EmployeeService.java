package com.acme.salary.employee;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for maintaining employees. Uniqueness is checked here to give a clear error; the database
 * constraints remain the final guard against concurrent duplicates.
 */
@Service
@Transactional
public class EmployeeService {

	private final EmployeeRepository repository;

	private final Clock clock;

	public EmployeeService(EmployeeRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
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

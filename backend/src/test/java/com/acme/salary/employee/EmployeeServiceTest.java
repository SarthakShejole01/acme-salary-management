package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.salary.currency.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);

	@Mock
	private EmployeeRepository repository;

	private EmployeeService service;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-01-15T09:30:00Z"), ZoneOffset.UTC);
		this.service = new EmployeeService(this.repository, clock);
	}

	@Test
	void creates_employee_with_salary_in_the_currency_of_their_country() {
		when(this.repository.save(any(Employee.class))).thenAnswer((invocation) -> invocation.getArgument(0));

		EmployeeResponse created = this.service.create(request("EMP-00001", "priya.sharma@acme.com", "2400000", TODAY));

		assertThat(created.employeeCode()).isEqualTo("EMP-00001");
		assertThat(created.fullName()).isEqualTo("Priya Sharma");
		assertThat(created.email()).isEqualTo("priya.sharma@acme.com");
		assertThat(created.jobTitle()).isEqualTo("Software Engineer");
		assertThat(created.department()).isEqualTo("Engineering");
		assertThat(created.country()).isEqualTo(Country.IN);
		assertThat(created.salary()).isEqualTo(new Money(new BigDecimal("2400000"), Currency.getInstance("INR")));
		assertThat(created.employmentType()).isEqualTo(EmploymentType.FULL_TIME);
		assertThat(created.hireDate()).isEqualTo(TODAY);
	}

	@Test
	void rejects_duplicate_employee_code() {
		when(this.repository.existsByEmployeeCode("EMP-00001")).thenReturn(true);

		assertThatThrownBy(() -> this.service.create(request("EMP-00001", "priya.sharma@acme.com", "50000", TODAY)))
				.isInstanceOf(DuplicateEmployeeException.class)
				.hasFieldOrPropertyWithValue("field", "employeeCode");
		verify(this.repository, never()).save(any());
	}

	@Test
	void rejects_email_already_used_by_another_employee() {
		when(this.repository.existsByEmailIgnoreCase("priya.sharma@acme.com")).thenReturn(true);

		assertThatThrownBy(() -> this.service.create(request("EMP-00001", "priya.sharma@acme.com", "50000", TODAY)))
				.isInstanceOf(DuplicateEmployeeException.class)
				.hasFieldOrPropertyWithValue("field", "email");
		verify(this.repository, never()).save(any());
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "0.00", "-1" })
	void rejects_salary_that_is_not_positive(String salary) {
		assertThatThrownBy(() -> this.service.create(request("EMP-00001", "priya.sharma@acme.com", salary, TODAY)))
				.isInstanceOf(InvalidEmployeeException.class)
				.hasFieldOrPropertyWithValue("field", "salary");
		verify(this.repository, never()).save(any());
	}

	@Test
	void rejects_hire_date_in_the_future() {
		LocalDate tomorrow = TODAY.plusDays(1);

		assertThatThrownBy(() -> this.service.create(request("EMP-00001", "priya.sharma@acme.com", "50000", tomorrow)))
				.isInstanceOf(InvalidEmployeeException.class)
				.hasFieldOrPropertyWithValue("field", "hireDate");
		verify(this.repository, never()).save(any());
	}

	@Test
	void accepts_hire_date_of_today() {
		when(this.repository.save(any(Employee.class))).thenAnswer((invocation) -> invocation.getArgument(0));

		EmployeeResponse created = this.service.create(request("EMP-00001", "priya.sharma@acme.com", "50000", TODAY));

		assertThat(created.hireDate()).isEqualTo(TODAY);
	}

	@Test
	void updates_all_details_of_an_existing_employee() {
		when(this.repository.findById(42L)).thenReturn(Optional.of(existingUsEmployee(42L)));
		EmployeeRequest changes = new EmployeeRequest("EMP-00099", "Priya Sharma-Rao", "priya.rao@acme.com",
				"Senior Software Engineer", "Platform", Country.IN, new BigDecimal("3100000"),
				EmploymentType.PART_TIME, LocalDate.of(2021, 6, 1));

		EmployeeResponse updated = this.service.update(42L, changes);

		assertThat(updated.id()).isEqualTo(42L);
		assertThat(updated.employeeCode()).isEqualTo("EMP-00099");
		assertThat(updated.fullName()).isEqualTo("Priya Sharma-Rao");
		assertThat(updated.email()).isEqualTo("priya.rao@acme.com");
		assertThat(updated.jobTitle()).isEqualTo("Senior Software Engineer");
		assertThat(updated.department()).isEqualTo("Platform");
		assertThat(updated.country()).isEqualTo(Country.IN);
		assertThat(updated.salary()).isEqualTo(new Money(new BigDecimal("3100000"), Currency.getInstance("INR")));
		assertThat(updated.employmentType()).isEqualTo(EmploymentType.PART_TIME);
		assertThat(updated.hireDate()).isEqualTo(LocalDate.of(2021, 6, 1));
	}

	@Test
	void rejects_update_of_an_employee_that_does_not_exist() {
		when(this.repository.findById(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> this.service.update(42L, request("EMP-00001", "priya.sharma@acme.com", "50000", TODAY)))
				.isInstanceOf(EmployeeNotFoundException.class)
				.hasMessageContaining("42");
	}

	@Test
	void rejects_update_to_a_code_used_by_another_employee() {
		when(this.repository.findById(42L)).thenReturn(Optional.of(existingUsEmployee(42L)));
		when(this.repository.existsByEmployeeCodeAndIdNot("EMP-00002", 42L)).thenReturn(true);

		assertThatThrownBy(() -> this.service.update(42L, request("EMP-00002", "priya.sharma@acme.com", "50000", TODAY)))
				.isInstanceOf(DuplicateEmployeeException.class)
				.hasFieldOrPropertyWithValue("field", "employeeCode");
	}

	@Test
	void rejects_update_to_an_email_used_by_another_employee() {
		when(this.repository.findById(42L)).thenReturn(Optional.of(existingUsEmployee(42L)));
		when(this.repository.existsByEmailIgnoreCaseAndIdNot("arjun.mehta@acme.com", 42L)).thenReturn(true);

		assertThatThrownBy(() -> this.service.update(42L, request("EMP-00001", "arjun.mehta@acme.com", "50000", TODAY)))
				.isInstanceOf(DuplicateEmployeeException.class)
				.hasFieldOrPropertyWithValue("field", "email");
	}

	@Test
	void applies_the_same_field_rules_on_update() {
		when(this.repository.findById(42L)).thenReturn(Optional.of(existingUsEmployee(42L)));

		assertThatThrownBy(() -> this.service.update(42L,
				request("EMP-00001", "priya.sharma@acme.com", "50000", TODAY.plusDays(1))))
				.isInstanceOf(InvalidEmployeeException.class)
				.hasFieldOrPropertyWithValue("field", "hireDate");
	}

	private static Employee existingUsEmployee(long id) {
		Employee employee = new Employee("EMP-00001", "Priya Sharma", "priya.sharma@acme.com", "Software Engineer",
				"Engineering", Country.US, new BigDecimal("120000"), EmploymentType.FULL_TIME, LocalDate.of(2020, 1, 6));
		ReflectionTestUtils.setField(employee, "id", id);
		return employee;
	}

	private static EmployeeRequest request(String code, String email, String salary, LocalDate hireDate) {
		return new EmployeeRequest(code, "Priya Sharma", email, "Software Engineer", "Engineering", Country.IN,
				new BigDecimal(salary), EmploymentType.FULL_TIME, hireDate);
	}

}

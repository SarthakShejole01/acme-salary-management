package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.salary.TestcontainersConfiguration;
import com.acme.salary.common.JpaAuditingConfiguration;
import com.acme.salary.currency.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, JpaAuditingConfiguration.class, EmployeeRepositoryTest.FixedClock.class })
class EmployeeRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-01-15T09:30:00Z");

	@Autowired
	private EmployeeRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void saves_and_reloads_all_employee_fields() {
		Employee saved = this.repository.saveAndFlush(employee("EMP-00001", "priya.sharma@acme.com", "2400000"));
		this.entityManager.clear();

		Employee reloaded = this.repository.findById(saved.getId()).orElseThrow();

		assertThat(reloaded.getEmployeeCode()).isEqualTo("EMP-00001");
		assertThat(reloaded.getFullName()).isEqualTo("Priya Sharma");
		assertThat(reloaded.getEmail()).isEqualTo("priya.sharma@acme.com");
		assertThat(reloaded.getJobTitle()).isEqualTo("Software Engineer");
		assertThat(reloaded.getDepartment()).isEqualTo("Engineering");
		assertThat(reloaded.getCountry()).isEqualTo(Country.IN);
		assertThat(reloaded.getSalary())
				.isEqualTo(new Money(new BigDecimal("2400000.00"), Currency.getInstance("INR")));
		assertThat(reloaded.getEmploymentType()).isEqualTo(EmploymentType.FULL_TIME);
		assertThat(reloaded.getHireDate()).isEqualTo(LocalDate.of(2022, 4, 1));
	}

	@Test
	void records_creation_and_update_time_from_the_clock() {
		Employee saved = this.repository.saveAndFlush(employee("EMP-00001", "priya.sharma@acme.com", "50000"));
		this.entityManager.clear();

		Employee reloaded = this.repository.findById(saved.getId()).orElseThrow();

		assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
		assertThat(reloaded.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void rejects_duplicate_employee_code() {
		this.repository.saveAndFlush(employee("EMP-00001", "first@acme.com", "50000"));

		assertThatThrownBy(() -> this.repository.saveAndFlush(employee("EMP-00001", "second@acme.com", "50000")))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uk_employee_code");
	}

	@Test
	void rejects_duplicate_email_ignoring_case() {
		this.repository.saveAndFlush(employee("EMP-00001", "priya.sharma@acme.com", "50000"));

		assertThatThrownBy(() -> this.repository.saveAndFlush(employee("EMP-00002", "Priya.Sharma@ACME.com", "50000")))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uk_employee_email");
	}

	@Test
	void rejects_zero_salary() {
		assertThatThrownBy(() -> this.repository.saveAndFlush(employee("EMP-00001", "zero@acme.com", "0")))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_employee_salary_positive");
	}

	@Test
	void finds_existing_email_ignoring_case() {
		this.repository.saveAndFlush(employee("EMP-00001", "priya.sharma@acme.com", "50000"));

		boolean exists = this.repository.existsByEmailIgnoreCase("Priya.Sharma@ACME.com");

		assertThat(exists).isTrue();
	}

	@Test
	void ignores_the_employee_itself_when_checking_code_and_email_for_update() {
		Employee saved = this.repository.saveAndFlush(employee("EMP-00001", "priya.sharma@acme.com", "50000"));

		boolean codeTaken = this.repository.existsByEmployeeCodeAndIdNot("EMP-00001", saved.getId());
		boolean emailTaken = this.repository.existsByEmailIgnoreCaseAndIdNot("PRIYA.SHARMA@acme.com", saved.getId());

		assertThat(codeTaken).isFalse();
		assertThat(emailTaken).isFalse();
	}

	@Test
	void detects_code_and_email_held_by_another_employee_for_update() {
		this.repository.saveAndFlush(employee("EMP-00001", "priya.sharma@acme.com", "50000"));
		Employee other = this.repository.saveAndFlush(employee("EMP-00002", "arjun.mehta@acme.com", "50000"));

		boolean codeTaken = this.repository.existsByEmployeeCodeAndIdNot("EMP-00001", other.getId());
		boolean emailTaken = this.repository.existsByEmailIgnoreCaseAndIdNot("priya.sharma@acme.com", other.getId());

		assertThat(codeTaken).isTrue();
		assertThat(emailTaken).isTrue();
	}

	@Test
	void lists_each_department_and_job_title_once_in_alphabetical_order() {
		this.repository.saveAllAndFlush(List.of(
				employee("EMP-00001", "a@acme.com", "Software Engineer", "Engineering"),
				employee("EMP-00002", "b@acme.com", "Data Analyst", "Finance"),
				employee("EMP-00003", "c@acme.com", "Software Engineer", "Engineering")));

		assertThat(this.repository.findDistinctDepartments()).containsExactly("Engineering", "Finance");
		assertThat(this.repository.findDistinctJobTitles()).containsExactly("Data Analyst", "Software Engineer");
	}

	private static Employee employee(String code, String email, String jobTitle, String department) {
		return new Employee(code, "Priya Sharma", email, jobTitle, department, Country.IN, new BigDecimal("50000"),
				EmploymentType.FULL_TIME, LocalDate.of(2022, 4, 1));
	}

	private static Employee employee(String code, String email, String salary) {
		return new Employee(code, "Priya Sharma", email, "Software Engineer", "Engineering", Country.IN,
				new BigDecimal(salary), EmploymentType.FULL_TIME, LocalDate.of(2022, 4, 1));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClock {

		@Bean
		Clock clock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}

	}

}

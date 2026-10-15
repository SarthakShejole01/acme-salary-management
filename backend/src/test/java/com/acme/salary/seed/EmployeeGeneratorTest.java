package com.acme.salary.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.employee.Country;
import com.acme.salary.employee.EmployeeRequest;
import com.acme.salary.employee.EmploymentType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EmployeeGeneratorTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 1, 15);

	private static final long SEED = 42L;

	private static final EmployeeGenerator generator = new EmployeeGenerator(NameLists.fromClasspath(), TODAY);

	private static List<EmployeeRequest> employees;

	@BeforeAll
	static void generateOnce() {
		employees = generator.generate(10_000, SEED);
	}

	@Test
	void generates_the_requested_number_of_employees() {
		assertThat(employees).hasSize(10_000);
	}

	@Test
	void generates_identical_employees_for_the_same_seed() {
		assertThat(generator.generate(10_000, SEED)).isEqualTo(employees);
	}

	@Test
	void generates_different_employees_for_a_different_seed() {
		assertThat(generator.generate(10_000, SEED + 1)).isNotEqualTo(employees);
	}

	@Test
	void numbers_employee_codes_sequentially_from_one() {
		assertThat(employees.getFirst().employeeCode()).isEqualTo("EMP-00001");
		assertThat(employees.getLast().employeeCode()).isEqualTo("EMP-10000");
		assertThat(employees).extracting(EmployeeRequest::employeeCode).doesNotHaveDuplicates();
	}

	@Test
	void gives_every_employee_a_unique_email_ignoring_case() {
		assertThat(employees).extracting((e) -> e.email().toLowerCase(Locale.ROOT)).doesNotHaveDuplicates();
	}

	@Test
	void builds_the_email_from_the_employees_name() {
		EmployeeRequest first = employees.getFirst();
		String[] name = first.fullName().toLowerCase(Locale.ROOT).split(" ");

		assertThat(first.email()).startsWith(name[0] + "." + name[1]).endsWith("@acme.com");
	}

	@Test
	void every_employee_passes_the_api_validation_rules() {
		try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
			Validator validator = factory.getValidator();

			assertThat(employees).allSatisfy((e) -> assertThat(validator.validate(e)).isEmpty());
		}
	}

	@Test
	void salaries_stay_within_the_band_for_title_country_and_employment_type() {
		assertThat(employees).allSatisfy((e) -> {
			SalaryModel.Range range = SalaryModel.range(e.jobTitle(), e.country(), e.employmentType());
			assertThat(e.salary()).isBetween(range.min(), range.max());
		});
	}

	@Test
	void pays_full_time_software_engineers_local_market_rates() {
		assertThat(salaries(Country.IN, "Software Engineer"))
			.isNotEmpty()
			.allSatisfy((s) -> assertThat(s).isBetween(new BigDecimal("1500000"), new BigDecimal("2500000")));
		assertThat(salaries(Country.US, "Software Engineer"))
			.isNotEmpty()
			.allSatisfy((s) -> assertThat(s).isBetween(new BigDecimal("90000"), new BigDecimal("130000")));
	}

	@Test
	void hire_dates_fall_within_the_last_15_years_and_never_after_today() {
		assertThat(employees).extracting(EmployeeRequest::hireDate)
			.allSatisfy((date) -> assertThat(date).isBetween(TODAY.minusYears(15), TODAY));
	}

	@Test
	void covers_every_country_and_every_department() {
		assertThat(employees).extracting(EmployeeRequest::country).contains(Country.values());
		assertThat(employees).extracting(EmployeeRequest::department)
			.containsAll(JobCatalogue.departments());
	}

	@Test
	void distributes_countries_roughly_by_weight() {
		long us = employees.stream().filter((e) -> e.country() == Country.US).count();
		long sg = employees.stream().filter((e) -> e.country() == Country.SG).count();

		assertThat(us).isBetween(2_700L, 3_300L);
		assertThat(sg).isBetween(250L, 550L);
	}

	private static List<BigDecimal> salaries(Country country, String jobTitle) {
		return employees.stream()
			.filter((e) -> e.country() == country && e.jobTitle().equals(jobTitle)
					&& e.employmentType() == EmploymentType.FULL_TIME)
			.map(EmployeeRequest::salary)
			.toList();
	}

}

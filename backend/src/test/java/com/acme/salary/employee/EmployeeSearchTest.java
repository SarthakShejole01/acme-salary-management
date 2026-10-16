package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.salary.TestcontainersConfiguration;
import com.acme.salary.common.ClockConfiguration;
import com.acme.salary.common.JpaAuditingConfiguration;
import com.acme.salary.common.PageResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Search runs through the real service and SQL on PostgreSQL. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({ TestcontainersConfiguration.class, ClockConfiguration.class, JpaAuditingConfiguration.class,
		EmployeeService.class })
class EmployeeSearchTest {

	private static final EmployeeSearchCriteria EVERYONE = new EmployeeSearchCriteria(null, null, null, null);

	@Autowired
	private EmployeeRepository repository;

	@Autowired
	private EmployeeService service;

	@BeforeEach
	void createEmployees() {
		this.repository.saveAllAndFlush(List.of(
				employee("EMP-00001", "Priya Sharma", "priya.sharma@acme.com", "Software Engineer", "Engineering",
						Country.IN, "2400000"),
				employee("EMP-00002", "Arjun Mehta", "arjun.mehta@acme.com", "Data Analyst", "Finance", Country.IN,
						"1500000"),
				employee("EMP-00003", "Emily Clark", "emily.clark@acme.com", "Software Engineer", "Engineering",
						Country.US, "130000"),
				employee("EMP-00004", "Lukas Weber", "lukas.weber@acme.com", "Software Engineer", "Engineering",
						Country.DE, "75000"),
				employee("EMP-00005", "Sophie Martin", "sophie_martin@acme.com", "HR Manager", "People", Country.FR,
						"60000")));
	}

	@Test
	void returns_the_requested_page_with_totals() {
		PageResponse<EmployeeResponse> page = this.service.search(EVERYONE,
				PageRequest.of(0, 2, Sort.by("fullName")));

		assertThat(names(page)).containsExactly("Arjun Mehta", "Emily Clark");
		assertThat(page.page()).isZero();
		assertThat(page.size()).isEqualTo(2);
		assertThat(page.totalElements()).isEqualTo(5);
		assertThat(page.totalPages()).isEqualTo(3);
	}

	@Test
	void search_matches_name_code_or_email_ignoring_case() {
		assertThat(names(search("SHARMA"))).containsExactly("Priya Sharma");
		assertThat(names(search("emp-00003"))).containsExactly("Emily Clark");
		assertThat(names(search("Lukas.Weber@"))).containsExactly("Lukas Weber");
	}

	@Test
	void search_treats_wildcard_characters_literally() {
		assertThat(names(search("_"))).containsExactly("Sophie Martin");
		assertThat(names(search("%"))).isEmpty();
	}

	@Test
	void blank_search_and_filters_match_everyone() {
		PageResponse<EmployeeResponse> page = this.service.search(new EmployeeSearchCriteria("  ", null, "", " "),
				Pageable.ofSize(10));

		assertThat(page.totalElements()).isEqualTo(5);
	}

	@Test
	void filters_by_country() {
		PageResponse<EmployeeResponse> page = this.service
			.search(new EmployeeSearchCriteria(null, Country.IN, null, null), Pageable.ofSize(10));

		assertThat(names(page)).containsExactly("Arjun Mehta", "Priya Sharma");
	}

	@Test
	void filters_by_department() {
		PageResponse<EmployeeResponse> page = this.service
			.search(new EmployeeSearchCriteria(null, null, "Finance", null), Pageable.ofSize(10));

		assertThat(names(page)).containsExactly("Arjun Mehta");
	}

	@Test
	void filters_by_job_title() {
		PageResponse<EmployeeResponse> page = this.service
			.search(new EmployeeSearchCriteria(null, null, null, "Software Engineer"), Pageable.ofSize(10));

		assertThat(names(page)).containsExactly("Emily Clark", "Lukas Weber", "Priya Sharma");
	}

	@Test
	void combines_search_with_filters() {
		PageResponse<EmployeeResponse> page = this.service
			.search(new EmployeeSearchCriteria("a", Country.IN, "Engineering", null), Pageable.ofSize(10));

		assertThat(names(page)).containsExactly("Priya Sharma");
	}

	@Test
	void sorts_by_the_requested_property() {
		PageResponse<EmployeeResponse> page = this.service.search(
				new EmployeeSearchCriteria(null, Country.IN, null, null),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "salary")));

		assertThat(names(page)).containsExactly("Priya Sharma", "Arjun Mehta");
	}

	@Test
	void sorts_by_name_when_no_sort_is_requested() {
		PageResponse<EmployeeResponse> page = this.service.search(EVERYONE, Pageable.ofSize(10));

		assertThat(names(page)).containsExactly("Arjun Mehta", "Emily Clark", "Lukas Weber", "Priya Sharma",
				"Sophie Martin");
	}

	@Test
	void rejects_sorting_by_an_unsupported_property() {
		assertThatThrownBy(() -> this.service.search(EVERYONE, PageRequest.of(0, 10, Sort.by("createdAt"))))
			.isInstanceOf(UnsupportedSortException.class)
			.hasFieldOrPropertyWithValue("field", "sort")
			.hasMessageContaining("createdAt");
	}

	private PageResponse<EmployeeResponse> search(String text) {
		return this.service.search(new EmployeeSearchCriteria(text, null, null, null), Pageable.ofSize(10));
	}

	private static List<String> names(PageResponse<EmployeeResponse> page) {
		return page.content().stream().map(EmployeeResponse::fullName).toList();
	}

	private static Employee employee(String code, String name, String email, String jobTitle, String department,
			Country country, String salary) {
		return new Employee(code, name, email, jobTitle, department, country, new BigDecimal(salary),
				EmploymentType.FULL_TIME, LocalDate.of(2022, 4, 1));
	}

}

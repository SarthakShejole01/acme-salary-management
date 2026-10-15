package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.acme.salary.currency.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

	private static final String VALID_REQUEST = """
			{
			  "employeeCode": "EMP-00001",
			  "fullName": "Priya Sharma",
			  "email": "priya.sharma@acme.com",
			  "jobTitle": "Software Engineer",
			  "department": "Engineering",
			  "country": "IN",
			  "salary": 2400000.00,
			  "employmentType": "FULL_TIME",
			  "hireDate": "2022-04-01"
			}
			""";

	private static final String EMPLOYEE_JSON = """
			{
			  "id": 42,
			  "employeeCode": "EMP-00001",
			  "fullName": "Priya Sharma",
			  "email": "priya.sharma@acme.com",
			  "jobTitle": "Software Engineer",
			  "department": "Engineering",
			  "country": "IN",
			  "salary": { "amount": 2400000.00, "currency": "INR" },
			  "employmentType": "FULL_TIME",
			  "hireDate": "2022-04-01"
			}
			""";

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private EmployeeService service;

	@Test
	void creates_employee_and_returns_201_with_its_location() {
		given(this.service.create(any(EmployeeRequest.class))).willReturn(employee(42L));

		assertThat(this.mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
				.hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", "http://localhost/api/employees/42")
				.bodyJson().isLenientlyEqualTo(EMPLOYEE_JSON);
	}

	@Test
	void passes_request_fields_to_the_service() {
		given(this.service.create(any(EmployeeRequest.class))).willReturn(employee(42L));

		this.mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST).exchange();

		verify(this.service).create(new EmployeeRequest("EMP-00001", "Priya Sharma", "priya.sharma@acme.com",
				"Software Engineer", "Engineering", Country.IN, new BigDecimal("2400000.00"), EmploymentType.FULL_TIME,
				LocalDate.of(2022, 4, 1)));
	}

	@Test
	void returns_an_employee_by_id() {
		given(this.service.get(42L)).willReturn(employee(42L));

		assertThat(this.mvc.get().uri("/api/employees/42"))
				.hasStatusOk()
				.bodyJson().isLenientlyEqualTo(EMPLOYEE_JSON);
	}

	@Test
	void updates_an_employee() {
		given(this.service.update(eq(42L), any(EmployeeRequest.class))).willReturn(employee(42L));

		assertThat(this.mvc.put().uri("/api/employees/42").contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
				.hasStatusOk()
				.bodyJson().isLenientlyEqualTo(EMPLOYEE_JSON);
	}

	@Test
	void deletes_an_employee_and_returns_204() {
		assertThat(this.mvc.delete().uri("/api/employees/42")).hasStatus(HttpStatus.NO_CONTENT);

		verify(this.service).delete(42L);
	}

	@Test
	void returns_404_problem_for_an_unknown_employee() {
		willThrow(new EmployeeNotFoundException(99L)).given(this.service).delete(99L);

		assertThat(this.mvc.delete().uri("/api/employees/99"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson().isLenientlyEqualTo("""
						{ "status": 404, "title": "Not Found", "detail": "Employee 99 does not exist" }
						""");
	}

	@Test
	void rejects_invalid_fields_with_400_listing_each_field() {
		String invalid = """
				{
				  "employeeCode": "EMP-00001",
				  "fullName": " ",
				  "email": "not-an-email",
				  "jobTitle": "Software Engineer",
				  "department": "Engineering",
				  "salary": 1000.123,
				  "employmentType": "FULL_TIME",
				  "hireDate": "2022-04-01"
				}
				""";

		assertThat(this.mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content(invalid))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
				.bodyJson()
				.extractingPath("$.errors[*].field")
				.asArray()
				.containsExactlyInAnyOrder("fullName", "email", "country", "salary");
		verifyNoInteractions(this.service);
	}

	@Test
	void rejects_non_positive_salary_with_400() {
		String zeroSalary = VALID_REQUEST.replace("2400000.00", "0");

		assertThat(this.mvc.put().uri("/api/employees/42").contentType(MediaType.APPLICATION_JSON).content(zeroSalary))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errors[*].field")
				.asArray()
				.containsExactly("salary");
		verifyNoInteractions(this.service);
	}

	@Test
	void rejects_unknown_country_code_with_400() {
		String unknownCountry = VALID_REQUEST.replace("\"IN\"", "\"XX\"");

		assertThat(this.mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content(unknownCountry))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		verifyNoInteractions(this.service);
	}

	private static EmployeeResponse employee(Long id) {
		return new EmployeeResponse(id, "EMP-00001", "Priya Sharma", "priya.sharma@acme.com", "Software Engineer",
				"Engineering", Country.IN, new Money(new BigDecimal("2400000"), Currency.getInstance("INR")),
				EmploymentType.FULL_TIME, LocalDate.of(2022, 4, 1));
	}

}

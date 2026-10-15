package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Full stack: HTTP → validation → service → PostgreSQL, committed for real (no test transaction). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EmployeeApiIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private EmployeeRepository repository;

	@BeforeEach
	void cleanDatabase() {
		this.repository.deleteAll();
	}

	@Test
	void persists_an_update_so_later_reads_see_it() {
		String location = createEmployee("EMP-00001", "priya.sharma@acme.com");

		assertThat(this.mvc.put().uri(location).contentType(MediaType.APPLICATION_JSON)
				.content(json("EMP-00001", "priya.sharma@acme.com").replace("Priya Sharma", "Priya Sharma-Rao")))
				.hasStatusOk();

		assertThat(this.mvc.get().uri(location))
				.hasStatusOk()
				.bodyJson().extractingPath("$.fullName").isEqualTo("Priya Sharma-Rao");
	}

	@Test
	void rejects_an_email_that_differs_only_in_case_with_409() {
		createEmployee("EMP-00001", "priya.sharma@acme.com");

		assertThat(this.mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON)
				.content(json("EMP-00002", "Priya.Sharma@ACME.com")))
				.hasStatus(HttpStatus.CONFLICT)
				.bodyJson().extractingPath("$.errors[0].field").isEqualTo("email");
	}

	@Test
	void returns_404_after_an_employee_is_deleted() {
		String location = createEmployee("EMP-00001", "priya.sharma@acme.com");

		assertThat(this.mvc.delete().uri(location)).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(this.mvc.get().uri(location)).hasStatus(HttpStatus.NOT_FOUND);
	}

	private String createEmployee(String code, String email) {
		var result = this.mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON)
				.content(json(code, email)).exchange();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return result.getResponse().getHeader("Location");
	}

	private static String json(String code, String email) {
		return """
				{
				  "employeeCode": "%s",
				  "fullName": "Priya Sharma",
				  "email": "%s",
				  "jobTitle": "Software Engineer",
				  "department": "Engineering",
				  "country": "IN",
				  "salary": 2400000,
				  "employmentType": "FULL_TIME",
				  "hireDate": "2022-04-01"
				}
				""".formatted(code, email);
	}

}

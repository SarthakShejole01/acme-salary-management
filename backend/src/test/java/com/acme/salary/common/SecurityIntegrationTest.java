package com.acme.salary.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The whole application with a shared HTTP basic-auth login configured, as in the deployed instance. */
@SpringBootTest(properties = { "app.security.username=hr", "app.security.password=correct horse battery staple" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void keeps_the_health_check_open_for_the_platform() {
		assertThat(this.mvc.get().uri("/actuator/health")).hasStatusOk();
	}

	@Test
	void challenges_api_requests_without_credentials() {
		assertThat(this.mvc.get().uri("/api/employees")).hasStatus(HttpStatus.UNAUTHORIZED)
			.hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"ACME Salary Management\"");
	}

	@Test
	void does_not_serve_the_app_itself_without_credentials() {
		assertThat(this.mvc.get().uri("/employees")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(this.mvc.get().uri("/index.html")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void rejects_a_wrong_password() {
		assertThat(this.mvc.get().uri("/api/employees").header(HttpHeaders.AUTHORIZATION, basic("hr", "wrong")))
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void rejects_a_wrong_username() {
		assertThat(this.mvc.get()
			.uri("/api/employees")
			.header(HttpHeaders.AUTHORIZATION, basic("someone", "correct horse battery staple")))
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void accepts_the_right_credentials_for_the_api_and_the_app() {
		String login = basic("hr", "correct horse battery staple");

		assertThat(this.mvc.get().uri("/api/employees").header(HttpHeaders.AUTHORIZATION, login)).hasStatusOk();
		assertThat(this.mvc.get().uri("/employees").header(HttpHeaders.AUTHORIZATION, login)).hasStatusOk();
	}

	@Test
	void lets_a_logged_in_browser_change_data_without_a_csrf_token() {
		// Writes need JSON, which a cross-site page cannot send without a CORS preflight that this API never answers.
		assertThat(this.mvc.post()
			.uri("/api/employees")
			.header(HttpHeaders.AUTHORIZATION, basic("hr", "correct horse battery staple"))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void refuses_writes_without_credentials_before_looking_at_the_body() {
		assertThat(this.mvc.post().uri("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void tells_browsers_not_to_store_salary_data() {
		assertThat(this.mvc.get()
			.uri("/api/employees")
			.header(HttpHeaders.AUTHORIZATION, basic("hr", "correct horse battery staple")))
			.headers()
			.hasValue(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate");
	}

	@Test
	void sends_standard_hardening_headers() {
		assertThat(this.mvc.get()
			.uri("/api/employees")
			.header(HttpHeaders.AUTHORIZATION, basic("hr", "correct horse battery staple")))
			.headers()
			.hasValue("X-Content-Type-Options", "nosniff")
			.hasValue("X-Frame-Options", "DENY");
	}

	private static String basic(String username, String password) {
		return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
	}

}

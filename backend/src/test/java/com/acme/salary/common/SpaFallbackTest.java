package com.acme.salary.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The Angular app is served from the same origin; routes it owns must survive a browser refresh. */
@WebMvcTest
@Import(SpaWebConfiguration.class)
class SpaFallbackTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void serves_the_app_at_the_root() {
		assertThat(this.mvc.get().uri("/")).hasStatusOk().bodyText().contains("ACME test app");
	}

	@Test
	void serves_the_app_for_a_client_side_route_so_refresh_and_deep_links_work() {
		assertThat(this.mvc.get().uri("/employees")).hasStatusOk()
			.hasContentTypeCompatibleWith(MediaType.TEXT_HTML)
			.bodyText()
			.contains("ACME test app");
		assertThat(this.mvc.get().uri("/insights/anything/deeper")).hasStatusOk().bodyText().contains("ACME test app");
	}

	@Test
	void serves_a_real_asset_as_it_is() {
		assertThat(this.mvc.get().uri("/main-test.js")).hasStatusOk().bodyText().contains("console.log('asset')");
	}

	@Test
	void does_not_hide_a_missing_file_behind_the_app() {
		assertThat(this.mvc.get().uri("/missing.js")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void keeps_unknown_api_paths_as_404_problems_not_html() {
		assertThat(this.mvc.get().uri("/api/nothing-here")).hasStatus(HttpStatus.NOT_FOUND)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void makes_browsers_revalidate_the_html_shell() {
		assertThat(this.mvc.get().uri("/employees")).headers().hasValue("Cache-Control", "no-cache");
	}

}

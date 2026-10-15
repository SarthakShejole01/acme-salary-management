package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ReferenceDataController.class)
class ReferenceDataControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ReferenceDataService service;

	@Test
	void returns_the_values_for_list_filters_and_the_employee_form() {
		given(this.service.referenceData()).willReturn(new ReferenceDataResponse(
				List.of(new ReferenceDataResponse.CountryOption("IN", "India", "INR")), List.of("Engineering"),
				List.of("Software Engineer"), List.of(EmploymentType.FULL_TIME)));

		assertThat(this.mvc.get().uri("/api/reference-data"))
			.hasStatusOk()
			.bodyJson().isStrictlyEqualTo("""
					{
					  "countries": [ { "code": "IN", "name": "India", "currency": "INR" } ],
					  "departments": [ "Engineering" ],
					  "jobTitles": [ "Software Engineer" ],
					  "employmentTypes": [ "FULL_TIME" ]
					}
					""");
	}

}

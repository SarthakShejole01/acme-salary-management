package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferenceDataServiceTest {

	@Mock
	private EmployeeRepository repository;

	@InjectMocks
	private ReferenceDataService service;

	@Test
	void lists_every_supported_country_by_name_with_its_currency() {
		ReferenceDataResponse data = this.service.referenceData();

		assertThat(data.countries()).extracting(ReferenceDataResponse.CountryOption::name)
			.containsExactly("Australia", "Canada", "France", "Germany", "India", "Singapore", "United Kingdom",
					"United States");
		assertThat(data.countries()).contains(new ReferenceDataResponse.CountryOption("IN", "India", "INR"));
	}

	@Test
	void lists_departments_and_job_titles_currently_in_use() {
		given(this.repository.findDistinctDepartments()).willReturn(List.of("Engineering", "Finance"));
		given(this.repository.findDistinctJobTitles()).willReturn(List.of("Data Analyst", "Software Engineer"));

		ReferenceDataResponse data = this.service.referenceData();

		assertThat(data.departments()).containsExactly("Engineering", "Finance");
		assertThat(data.jobTitles()).containsExactly("Data Analyst", "Software Engineer");
	}

	@Test
	void lists_every_employment_type() {
		ReferenceDataResponse data = this.service.referenceData();

		assertThat(data.employmentTypes()).containsExactly(EmploymentType.values());
	}

}

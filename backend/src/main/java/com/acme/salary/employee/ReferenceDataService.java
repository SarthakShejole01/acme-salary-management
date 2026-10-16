package com.acme.salary.employee;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Countries and employment types are fixed by the code; departments and job titles are whatever is currently
 * in use, so filters never offer a value that matches nobody.
 */
@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

	private final EmployeeRepository repository;

	public ReferenceDataService(EmployeeRepository repository) {
		this.repository = repository;
	}

	public ReferenceDataResponse referenceData() {
		List<ReferenceDataResponse.CountryOption> countries = Arrays.stream(Country.values())
			.map(ReferenceDataResponse.CountryOption::of)
			.sorted(Comparator.comparing(ReferenceDataResponse.CountryOption::name))
			.toList();
		return new ReferenceDataResponse(countries, this.repository.findDistinctDepartments(),
				this.repository.findDistinctJobTitles(), List.of(EmploymentType.values()));
	}

}

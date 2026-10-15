package com.acme.salary.employee;

import java.util.List;

/** Values for the list filters and the employee form. */
public record ReferenceDataResponse(List<CountryOption> countries, List<String> departments, List<String> jobTitles,
		List<EmploymentType> employmentTypes) {

	public record CountryOption(String code, String name, String currency) {

		static CountryOption of(Country country) {
			return new CountryOption(country.name(), country.displayName(), country.currency().getCurrencyCode());
		}

	}

}

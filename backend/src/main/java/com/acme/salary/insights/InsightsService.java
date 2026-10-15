package com.acme.salary.insights;

import com.acme.salary.employee.Country;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InsightsService {

	private final InsightsQueries queries;

	public InsightsService(InsightsQueries queries) {
		this.queries = queries;
	}

	public List<CountryInsight> countryInsights() {
		return this.queries.countryInsights()
			.stream()
			.sorted(Comparator.comparing(CountryInsight::countryName))
			.toList();
	}

	public List<JobTitleInsight> jobTitleInsights(Country country) {
		return this.queries.jobTitleInsights(country);
	}

}

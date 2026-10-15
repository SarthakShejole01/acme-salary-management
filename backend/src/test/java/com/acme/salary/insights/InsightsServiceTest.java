package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.acme.salary.currency.Money;
import com.acme.salary.employee.Country;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

	@Mock
	private InsightsQueries queries;

	@InjectMocks
	private InsightsService service;

	@Test
	void lists_country_insights_alphabetically_by_country_name() {
		given(this.queries.countryInsights())
			.willReturn(List.of(insight(Country.US), insight(Country.GB), insight(Country.IN)));

		List<CountryInsight> insights = this.service.countryInsights();

		assertThat(insights).extracting(CountryInsight::countryName)
			.containsExactly("India", "United Kingdom", "United States");
	}

	private static CountryInsight insight(Country country) {
		Money amount = new Money(BigDecimal.TEN, country.currency());
		return new CountryInsight(country.name(), country.displayName(), 1, amount, amount, amount, amount);
	}

}

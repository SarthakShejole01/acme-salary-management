package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CountryTest {

	@ParameterizedTest(name = "{0} pays in {1}")
	@CsvSource({
			"US, USD",
			"GB, GBP",
			"IN, INR",
			"DE, EUR",
			"FR, EUR",
			"CA, CAD",
			"AU, AUD",
			"SG, SGD",
	})
	void maps_country_to_its_local_currency(Country country, String currencyCode) {
		Currency currency = country.currency();

		assertThat(currency).isEqualTo(Currency.getInstance(currencyCode));
	}

	@Test
	void supports_exactly_the_eight_v1_countries() {
		assertThat(Country.values())
				.extracting(Country::name)
				.containsExactlyInAnyOrder("US", "GB", "IN", "DE", "FR", "CA", "AU", "SG");
	}

}

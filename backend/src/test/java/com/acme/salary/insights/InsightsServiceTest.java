package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.acme.salary.currency.ExchangeRateRepository;
import com.acme.salary.currency.ExchangeRates;
import com.acme.salary.currency.Money;
import com.acme.salary.employee.Country;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

	private static final Currency GBP = Currency.getInstance("GBP");

	@Mock
	private InsightsQueries queries;

	@Mock
	private ExchangeRateRepository exchangeRates;

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

	@Test
	void splits_a_country_into_round_buckets_and_keeps_the_empty_ones() {
		given(this.queries.salaryRange(Country.GB))
			.willReturn(Optional.of(new SalaryRange(new BigDecimal("16000"), new BigDecimal("109500"), GBP)));
		given(this.queries.bucketCounts(eq(Country.GB), any())).willReturn(Map.of(1, 3L, 10, 1L));

		SalaryDistribution distribution = this.service.distribution(Country.GB);

		assertThat(distribution.currency()).isEqualTo("GBP");
		assertThat(distribution.buckets()).hasSize(10);
		assertThat(distribution.buckets().getFirst()).isEqualTo(new DistributionBucket(gbp("10000"), gbp("20000"), 3));
		assertThat(distribution.buckets().subList(1, 9)).allSatisfy((b) -> assertThat(b.headcount()).isZero());
		assertThat(distribution.buckets().getLast())
			.isEqualTo(new DistributionBucket(gbp("100000"), gbp("110000"), 1));
	}

	@Test
	void has_no_buckets_for_a_country_without_employees() {
		given(this.queries.salaryRange(Country.SG)).willReturn(Optional.empty());

		SalaryDistribution distribution = this.service.distribution(Country.SG);

		assertThat(distribution.currency()).isEqualTo("SGD");
		assertThat(distribution.buckets()).isEmpty();
	}

	@Test
	void puts_everyone_in_one_bucket_when_all_salaries_are_equal() {
		given(this.queries.salaryRange(Country.GB))
			.willReturn(Optional.of(new SalaryRange(new BigDecimal("50000"), new BigDecimal("50000"), GBP)));
		given(this.queries.bucketCounts(eq(Country.GB), any())).willReturn(Map.of(1, 4L));

		SalaryDistribution distribution = this.service.distribution(Country.GB);

		assertThat(distribution.buckets()).singleElement()
			.satisfies((bucket) -> {
				assertThat(bucket.headcount()).isEqualTo(4);
				assertThat(bucket.from()).isEqualTo(gbp("50000"));
			});
	}

	@Test
	void totals_the_organisation_in_usd_converting_each_currency_first() {
		given(this.exchangeRates.currentRates()).willReturn(rates());
		given(this.queries.salaryTotalsByCurrency()).willReturn(List.of(
				new CurrencyTotal(2, money("4000000", "INR")), new CurrencyTotal(1, money("100000", "USD")),
				new CurrencyTotal(1, money("50000", "GBP"))));

		OrgSummary summary = this.service.summary();

		// 4,000,000 x 0.01050894 + 100,000 + 50,000 x 1.32665758 = 208,368.639 USD
		assertThat(summary.headcount()).isEqualTo(4);
		assertThat(summary.totalAnnualCost()).isEqualTo(money("208368.64", "USD"));
		assertThat(summary.averageSalary()).isEqualTo(money("52092.16", "USD"));
	}

	@Test
	void labels_the_summary_approximate_with_the_rate_date() {
		given(this.exchangeRates.currentRates()).willReturn(rates());
		given(this.queries.salaryTotalsByCurrency()).willReturn(List.of(new CurrencyTotal(1, money("100000", "USD"))));

		OrgSummary summary = this.service.summary();

		assertThat(summary.approximate()).isTrue();
		assertThat(summary.ratesAsOf()).isEqualTo(LocalDate.of(2026, 10, 14));
	}

	@Test
	void has_zero_cost_and_no_average_without_employees() {
		given(this.exchangeRates.currentRates()).willReturn(rates());
		given(this.queries.salaryTotalsByCurrency()).willReturn(List.of());

		OrgSummary summary = this.service.summary();

		assertThat(summary.headcount()).isZero();
		assertThat(summary.totalAnnualCost()).isEqualTo(money("0", "USD"));
		assertThat(summary.averageSalary()).isNull();
	}

	private static ExchangeRates rates() {
		return new ExchangeRates(Map.of(Currency.getInstance("USD"), BigDecimal.ONE, Currency.getInstance("INR"),
				new BigDecimal("0.01050894"), GBP, new BigDecimal("1.32665758")), LocalDate.of(2026, 10, 14));
	}

	private static Money money(String amount, String currency) {
		return new Money(new BigDecimal(amount), Currency.getInstance(currency));
	}

	private static Money gbp(String amount) {
		return new Money(new BigDecimal(amount), GBP);
	}

	private static CountryInsight insight(Country country) {
		Money amount = new Money(BigDecimal.TEN, country.currency());
		return new CountryInsight(country.name(), country.displayName(), 1, amount, amount, amount, amount);
	}

}

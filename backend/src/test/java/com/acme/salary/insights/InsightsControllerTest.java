package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.acme.salary.currency.Money;
import com.acme.salary.employee.Country;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(InsightsController.class)
class InsightsControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private InsightsService service;

	@Test
	void returns_country_insights_with_amounts_in_local_currency() {
		Currency inr = Currency.getInstance("INR");
		given(this.service.countryInsights()).willReturn(List.of(new CountryInsight("IN", "India", 3,
				new Money(new BigDecimal("500000"), inr), new Money(new BigDecimal("3600000"), inr),
				new Money(new BigDecimal("1766666.67"), inr), new Money(new BigDecimal("1200000"), inr))));

		assertThat(this.mvc.get().uri("/api/insights/countries"))
			.hasStatusOk()
			.bodyJson().isStrictlyEqualTo("""
					[
					  {
					    "countryCode": "IN",
					    "countryName": "India",
					    "headcount": 3,
					    "min":     { "amount": 500000.00,  "currency": "INR" },
					    "max":     { "amount": 3600000.00, "currency": "INR" },
					    "average": { "amount": 1766666.67, "currency": "INR" },
					    "median":  { "amount": 1200000.00, "currency": "INR" }
					  }
					]
					""");
	}

	@Test
	void returns_job_title_insights_for_one_country() {
		Currency inr = Currency.getInstance("INR");
		Money amount = new Money(new BigDecimal("1750000"), inr);
		given(this.service.jobTitleInsights(Country.IN))
			.willReturn(List.of(new JobTitleInsight("Software Engineer", 4, amount, amount, amount, amount)));

		assertThat(this.mvc.get().uri("/api/insights/countries/IN/job-titles"))
			.hasStatusOk()
			.bodyJson().isLenientlyEqualTo("""
					[ { "jobTitle": "Software Engineer", "headcount": 4,
					    "median": { "amount": 1750000.00, "currency": "INR" } } ]
					""");
	}

	@Test
	void rejects_an_unknown_country_code_with_400() {
		assertThat(this.mvc.get().uri("/api/insights/countries/XX/job-titles"))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void returns_the_salary_distribution_of_one_country() {
		Currency gbp = Currency.getInstance("GBP");
		given(this.service.distribution(Country.GB)).willReturn(new SalaryDistribution("GB", "GBP",
				List.of(new DistributionBucket(new Money(new BigDecimal("10000"), gbp),
						new Money(new BigDecimal("20000"), gbp), 3))));

		assertThat(this.mvc.get().uri("/api/insights/countries/GB/distribution"))
			.hasStatusOk()
			.bodyJson().isStrictlyEqualTo("""
					{
					  "countryCode": "GB",
					  "currency": "GBP",
					  "buckets": [
					    { "from": { "amount": 10000.00, "currency": "GBP" },
					      "to":   { "amount": 20000.00, "currency": "GBP" },
					      "headcount": 3 }
					  ]
					}
					""");
	}

	@Test
	void returns_the_org_summary_in_usd_labelled_approximate() {
		Currency usd = Currency.getInstance("USD");
		given(this.service.summary()).willReturn(new OrgSummary(10_000, new Money(new BigDecimal("950000000"), usd),
				new Money(new BigDecimal("95000"), usd), true, LocalDate.of(2026, 10, 14)));

		assertThat(this.mvc.get().uri("/api/insights/summary"))
			.hasStatusOk()
			.bodyJson().isStrictlyEqualTo("""
					{
					  "headcount": 10000,
					  "totalAnnualCost": { "amount": 950000000.00, "currency": "USD" },
					  "averageSalary":   { "amount": 95000.00,     "currency": "USD" },
					  "approximate": true,
					  "ratesAsOf": "2026-10-14"
					}
					""");
	}

}

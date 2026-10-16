package com.acme.salary.insights;

import com.acme.salary.currency.ExchangeRateRepository;
import com.acme.salary.currency.ExchangeRates;
import com.acme.salary.currency.Money;
import com.acme.salary.employee.Country;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InsightsService {

	/** Aim for about ten bars; nice rounding of the width gives between roughly 5 and 11. */
	private static final int TARGET_BUCKETS = 10;

	private final InsightsQueries queries;

	private final ExchangeRateRepository exchangeRates;

	public InsightsService(InsightsQueries queries, ExchangeRateRepository exchangeRates) {
		this.queries = queries;
		this.exchangeRates = exchangeRates;
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

	public SalaryDistribution distribution(Country country) {
		String currency = country.currency().getCurrencyCode();
		Optional<SalaryRange> range = this.queries.salaryRange(country);
		if (range.isEmpty()) {
			return new SalaryDistribution(country.name(), currency, List.of());
		}
		HistogramBins bins = HistogramBins.covering(range.get().min(), range.get().max(), TARGET_BUCKETS);
		Map<Integer, Long> counts = this.queries.bucketCounts(country, bins);
		List<DistributionBucket> buckets = IntStream.rangeClosed(1, bins.count())
			.mapToObj((bucket) -> new DistributionBucket(new Money(bins.lowerEdge(bucket), country.currency()),
					new Money(bins.upperEdge(bucket), country.currency()), counts.getOrDefault(bucket, 0L)))
			.toList();
		return new SalaryDistribution(country.name(), currency, buckets);
	}

	/**
	 * Converts each currency's salary total to USD (exactly), adds them, and rounds once to cents. Salaries in
	 * different currencies are never added before conversion.
	 */
	public OrgSummary summary() {
		ExchangeRates rates = this.exchangeRates.currentRates();
		long headcount = 0;
		BigDecimal totalUsd = BigDecimal.ZERO;
		for (CurrencyTotal total : this.queries.salaryTotalsByCurrency()) {
			headcount += total.headcount();
			totalUsd = totalUsd.add(rates.usdValue(total.total()));
		}
		Money totalCost = new Money(totalUsd.setScale(2, RoundingMode.HALF_UP), ExchangeRates.USD);
		Money average = (headcount == 0) ? null
				: new Money(totalUsd.divide(BigDecimal.valueOf(headcount), 2, RoundingMode.HALF_UP), ExchangeRates.USD);
		return new OrgSummary(headcount, totalCost, average, true, rates.asOf());
	}

}

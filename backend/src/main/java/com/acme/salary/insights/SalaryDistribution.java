package com.acme.salary.insights;

import java.util.List;

/** Salary histogram for one country, in its own currency. Buckets are contiguous and may have zero headcount. */
public record SalaryDistribution(String countryCode, String currency, List<DistributionBucket> buckets) {

}

package com.acme.salary.insights;

import com.acme.salary.currency.Money;
import java.time.LocalDate;

/**
 * Organisation-wide totals in USD. Always {@code approximate}: each currency is converted at the fixed rate
 * dated {@code ratesAsOf}. {@code averageSalary} is null when there are no employees.
 */
public record OrgSummary(long headcount, Money totalAnnualCost, Money averageSalary, boolean approximate,
		LocalDate ratesAsOf) {

}

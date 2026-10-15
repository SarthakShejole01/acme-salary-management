package com.acme.salary.insights;

import com.acme.salary.currency.Money;

/** Pay statistics for one country. All amounts are in that country's own currency. */
public record CountryInsight(String countryCode, String countryName, long headcount, Money min, Money max,
		Money average, Money median) {

}

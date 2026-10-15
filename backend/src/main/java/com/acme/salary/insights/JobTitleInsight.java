package com.acme.salary.insights;

import com.acme.salary.currency.Money;

/** Pay statistics for one job title within one country, in that country's currency. */
public record JobTitleInsight(String jobTitle, long headcount, Money min, Money max, Money average, Money median) {

}

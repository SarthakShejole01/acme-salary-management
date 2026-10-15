package com.acme.salary.insights;

import com.acme.salary.currency.Money;

/** Employees earning at least {@code from} and less than {@code to}. */
public record DistributionBucket(Money from, Money to, long headcount) {

}

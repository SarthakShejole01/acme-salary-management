package com.acme.salary.insights;

import com.acme.salary.currency.Money;

/** Headcount and summed salaries of everyone paid in one currency. */
record CurrencyTotal(long headcount, Money total) {

}

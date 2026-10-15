package com.acme.salary.insights;

import java.math.BigDecimal;
import java.util.Currency;

/** Lowest and highest salary in one country, and that country's currency. */
record SalaryRange(BigDecimal min, BigDecimal max, Currency currency) {

}

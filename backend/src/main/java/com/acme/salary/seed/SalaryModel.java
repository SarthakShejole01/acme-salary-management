package com.acme.salary.seed;

import com.acme.salary.employee.Country;
import com.acme.salary.employee.EmploymentType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Seed salary = US base pay for the title x country pay factor x (1 +/- up to 15% noise) x part-time factor,
 * rounded to a whole local unit. The country factor already includes the currency, so the result is local
 * money (an Indian software engineer earns about INR 20 lakh, not USD 110,000). Pure BigDecimal arithmetic.
 */
final class SalaryModel {

	/** Maximum noise, in basis points of the banded salary (1500 = 15%). */
	static final int MAX_NOISE_BASIS_POINTS = 1500;

	private static final BigDecimal PART_TIME_FACTOR = new BigDecimal("0.6");

	private record CountryPay(BigDecimal factor, BigDecimal roundTo) {

	}

	private static final Map<Country, CountryPay> PAY = new EnumMap<>(Map.of(
			Country.US, pay("1.00", 100),
			Country.GB, pay("0.55", 100),
			Country.IN, pay("18", 1000),
			Country.DE, pay("0.62", 100),
			Country.FR, pay("0.55", 100),
			Country.CA, pay("0.95", 100),
			Country.AU, pay("1.05", 100),
			Country.SG, pay("1.05", 100)));

	record Range(BigDecimal min, BigDecimal max) {

	}

	private SalaryModel() {
	}

	static BigDecimal salary(JobCatalogue.Job job, Country country, EmploymentType type, int noiseBasisPoints) {
		CountryPay pay = PAY.get(country);
		BigDecimal noise = BigDecimal.ONE.add(BigDecimal.valueOf(noiseBasisPoints, 4));
		BigDecimal raw = job.usBasePay().multiply(pay.factor()).multiply(noise).multiply(typeFactor(type));
		return raw.divide(pay.roundTo(), 0, RoundingMode.HALF_EVEN).multiply(pay.roundTo()).setScale(2);
	}

	/** Lowest and highest salary the model can produce for this combination. */
	static Range range(String jobTitle, Country country, EmploymentType type) {
		JobCatalogue.Job job = JobCatalogue.byTitle(jobTitle);
		return new Range(salary(job, country, type, -MAX_NOISE_BASIS_POINTS),
				salary(job, country, type, MAX_NOISE_BASIS_POINTS));
	}

	private static BigDecimal typeFactor(EmploymentType type) {
		return (type == EmploymentType.PART_TIME) ? PART_TIME_FACTOR : BigDecimal.ONE;
	}

	private static CountryPay pay(String factor, int roundTo) {
		return new CountryPay(new BigDecimal(factor), BigDecimal.valueOf(roundTo));
	}

}

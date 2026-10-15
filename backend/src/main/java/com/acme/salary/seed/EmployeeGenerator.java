package com.acme.salary.seed;

import com.acme.salary.employee.Country;
import com.acme.salary.employee.EmployeeRequest;
import com.acme.salary.employee.EmploymentType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Generates realistic employees for seeding. Pure: the same names, "today" and seed always give the same list,
 * because every random choice comes from one {@link Random} seeded by the caller.
 */
public final class EmployeeGenerator {

	private static final int HIRE_WINDOW_DAYS = 15 * 365;

	private static final WeightedChoice<Country> COUNTRIES = WeightedChoice.of(
			List.of(Country.US, Country.IN, Country.GB, Country.DE, Country.CA, Country.FR, Country.AU, Country.SG),
			EmployeeGenerator::countryWeight);

	private static final WeightedChoice<JobCatalogue.Job> JOBS = WeightedChoice.of(JobCatalogue.JOBS,
			JobCatalogue.Job::weight);

	private static final WeightedChoice<EmploymentType> EMPLOYMENT_TYPES = WeightedChoice.of(
			List.of(EmploymentType.FULL_TIME, EmploymentType.PART_TIME, EmploymentType.CONTRACTOR),
			(type) -> switch (type) {
				case FULL_TIME -> 88;
				case PART_TIME -> 7;
				case CONTRACTOR -> 5;
			});

	private final NameLists names;

	private final LocalDate today;

	public EmployeeGenerator(NameLists names, LocalDate today) {
		this.names = names;
		this.today = today;
	}

	public List<EmployeeRequest> generate(int count, long seed) {
		Random random = new Random(seed);
		Map<String, Integer> emailsInUse = new HashMap<>();
		List<EmployeeRequest> employees = new ArrayList<>(count);
		for (int number = 1; number <= count; number++) {
			Country country = COUNTRIES.pick(random);
			JobCatalogue.Job job = JOBS.pick(random);
			EmploymentType type = EMPLOYMENT_TYPES.pick(random);
			String firstName = pick(this.names.firstNames(), random);
			String lastName = pick(this.names.lastNames(), random);
			int noise = random.nextInt(2 * SalaryModel.MAX_NOISE_BASIS_POINTS + 1) - SalaryModel.MAX_NOISE_BASIS_POINTS;
			BigDecimal salary = SalaryModel.salary(job, country, type, noise);
			LocalDate hireDate = this.today.minusDays(random.nextInt(HIRE_WINDOW_DAYS + 1));
			employees.add(new EmployeeRequest("EMP-%05d".formatted(number), firstName + " " + lastName,
					uniqueEmail(firstName, lastName, emailsInUse), job.title(), job.department(), country, salary,
					type, hireDate));
		}
		return employees;
	}

	/** first.last@acme.com, then first.last2@acme.com, first.last3@... for later people with the same name. */
	private static String uniqueEmail(String firstName, String lastName, Map<String, Integer> emailsInUse) {
		String localPart = (firstName + "." + lastName).toLowerCase(Locale.ROOT);
		int occurrence = emailsInUse.merge(localPart, 1, Integer::sum);
		return ((occurrence == 1) ? localPart : localPart + occurrence) + "@acme.com";
	}

	private static String pick(List<String> values, Random random) {
		return values.get(random.nextInt(values.size()));
	}

	private static int countryWeight(Country country) {
		return switch (country) {
			case US -> 30;
			case IN -> 25;
			case GB -> 12;
			case DE -> 10;
			case CA -> 8;
			case FR -> 6;
			case AU -> 5;
			case SG -> 4;
		};
	}

}

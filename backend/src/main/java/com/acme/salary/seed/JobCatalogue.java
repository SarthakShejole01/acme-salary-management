package com.acme.salary.seed;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Departments and job titles used for seed data. {@code usBasePay} is a typical annual base salary in the
 * United States (USD); {@link SalaryModel} turns it into local pay. {@code weight} sets how common a title is.
 */
final class JobCatalogue {

	record Job(String department, String title, BigDecimal usBasePay, int weight) {

	}

	static final List<Job> JOBS = List.of(
			job("Engineering", "Software Engineer", 110_000, 20),
			job("Engineering", "Senior Software Engineer", 145_000, 10),
			job("Engineering", "Engineering Manager", 175_000, 3),
			job("Engineering", "QA Engineer", 90_000, 5),
			job("Engineering", "DevOps Engineer", 120_000, 4),
			job("Data", "Data Analyst", 85_000, 5),
			job("Data", "Data Scientist", 125_000, 3),
			job("Product", "Product Manager", 135_000, 4),
			job("Product", "Product Designer", 110_000, 3),
			job("Sales", "Account Executive", 90_000, 8),
			job("Sales", "Sales Manager", 125_000, 2),
			job("Marketing", "Marketing Specialist", 75_000, 4),
			job("Finance", "Accountant", 75_000, 3),
			job("Finance", "Financial Analyst", 85_000, 3),
			job("People", "HR Generalist", 70_000, 2),
			job("People", "Recruiter", 70_000, 2),
			job("People", "HR Manager", 100_000, 1),
			job("Customer Support", "Support Specialist", 55_000, 10),
			job("Customer Support", "Support Team Lead", 75_000, 2));

	private JobCatalogue() {
	}

	static Job byTitle(String title) {
		return JOBS.stream()
			.filter((job) -> job.title().equals(title))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown job title: " + title));
	}

	static Set<String> departments() {
		Set<String> departments = new LinkedHashSet<>();
		JOBS.forEach((job) -> departments.add(job.department()));
		return departments;
	}

	private static Job job(String department, String title, int usBasePay, int weight) {
		return new Job(department, title, BigDecimal.valueOf(usBasePay), weight);
	}

}

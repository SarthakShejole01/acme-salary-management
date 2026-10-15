package com.acme.salary.seed;

/** What a seed run did, and how many employees the database holds afterwards. */
public record SeedResult(Outcome outcome, long employeeCount) {

	public enum Outcome {

		SEEDED,
		SKIPPED

	}

}

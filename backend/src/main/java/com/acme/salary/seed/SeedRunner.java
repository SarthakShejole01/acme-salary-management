package com.acme.salary.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds the database when the application is started with the {@code seed} profile, then the process exits
 * (the profile disables the web server):
 * {@code java -jar salary.jar --spring.profiles.active=seed [--reset]}.
 */
@Component
@Profile("seed")
class SeedRunner implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(SeedRunner.class);

	private final EmployeeSeeder seeder;

	SeedRunner(EmployeeSeeder seeder) {
		this.seeder = seeder;
	}

	@Override
	public void run(ApplicationArguments args) {
		boolean reset = args.containsOption("reset");
		long start = System.nanoTime();
		SeedResult result = this.seeder.seed(reset);
		long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
		switch (result.outcome()) {
			case SEEDED -> logger.info("Seeded {} employees in {} ms{}", result.employeeCount(), elapsedMillis,
					reset ? " (after reset)" : "");
			case SKIPPED -> logger.info("Skipped seeding: {} employees already exist. Run with --reset to replace them.",
					result.employeeCount());
		}
	}

}

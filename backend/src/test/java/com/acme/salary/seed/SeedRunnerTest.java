package com.acme.salary.seed;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class SeedRunnerTest {

	@Mock
	private EmployeeSeeder seeder;

	@InjectMocks
	private SeedRunner runner;

	@Test
	void seeds_without_reset_by_default() {
		given(this.seeder.seed(false)).willReturn(new SeedResult(SeedResult.Outcome.SEEDED, 10_000));

		this.runner.run(new DefaultApplicationArguments("--spring.profiles.active=seed"));

		verify(this.seeder).seed(false);
	}

	@Test
	void resets_when_the_reset_option_is_given() {
		given(this.seeder.seed(true)).willReturn(new SeedResult(SeedResult.Outcome.SEEDED, 10_000));

		this.runner.run(new DefaultApplicationArguments("--spring.profiles.active=seed", "--reset"));

		verify(this.seeder).seed(true);
	}

}

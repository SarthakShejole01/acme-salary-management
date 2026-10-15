package com.acme.salary.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameListsTest {

	private final NameLists names = NameLists.fromClasspath();

	@Test
	void loads_enough_first_and_last_names_for_realistic_variety() {
		assertThat(this.names.firstNames()).hasSizeGreaterThanOrEqualTo(50);
		assertThat(this.names.lastNames()).hasSizeGreaterThanOrEqualTo(50);
	}

	@Test
	void names_are_unique_trimmed_words_usable_in_an_email_address() {
		assertThat(this.names.firstNames()).doesNotHaveDuplicates().allMatch((name) -> name.matches("[A-Z][a-z]+"));
		assertThat(this.names.lastNames()).doesNotHaveDuplicates().allMatch((name) -> name.matches("[A-Z][a-z]+"));
	}

}

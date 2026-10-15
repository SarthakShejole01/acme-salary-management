package com.acme.salary.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AppSecurityPropertiesTest {

	@Test
	void is_enabled_when_a_username_and_password_are_given() {
		assertThat(new AppSecurityProperties("hr", "s3cret").enabled()).isTrue();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "  " })
	void is_disabled_when_neither_is_given(String blank) {
		assertThat(new AppSecurityProperties(blank, blank).enabled()).isFalse();
	}

	@Test
	void refuses_a_username_without_a_password() {
		assertThatThrownBy(() -> new AppSecurityProperties("hr", "").validate(false))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("both");
	}

	@Test
	void refuses_a_password_without_a_username() {
		assertThatThrownBy(() -> new AppSecurityProperties(null, "s3cret").validate(false))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("both");
	}

	@Test
	void allows_running_without_credentials_outside_production() {
		assertThatCode(() -> new AppSecurityProperties(null, null).validate(false)).doesNotThrowAnyException();
	}

	@Test
	void refuses_to_start_in_production_without_credentials() {
		assertThatThrownBy(() -> new AppSecurityProperties(null, null).validate(true))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("prod")
			.hasMessageContaining("APP_SECURITY_USERNAME");
	}

	@Test
	void starts_in_production_with_credentials() {
		assertThatCode(() -> new AppSecurityProperties("hr", "s3cret").validate(true)).doesNotThrowAnyException();
	}

	@Test
	void does_not_reveal_the_password_when_printed() {
		assertThat(new AppSecurityProperties("hr", "s3cret").toString()).doesNotContain("s3cret");
	}

}

package com.acme.salary.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * The single shared HTTP basic-auth login (ADR 005), read from {@code APP_SECURITY_USERNAME} and
 * {@code APP_SECURITY_PASSWORD}. Leave both unset to run without a login (local development and tests).
 */
@ConfigurationProperties("app.security")
public record AppSecurityProperties(String username, String password) {

	public boolean enabled() {
		return StringUtils.hasText(this.username) && StringUtils.hasText(this.password);
	}

	/**
	 * Fails fast on a half-configured login, and in the {@code prod} profile on a missing one, so a deployment can
	 * never start by accident with salary data open to everyone.
	 */
	public void validate(boolean production) {
		boolean hasUsername = StringUtils.hasText(this.username);
		boolean hasPassword = StringUtils.hasText(this.password);
		if (hasUsername != hasPassword) {
			throw new IllegalStateException(
					"Set both APP_SECURITY_USERNAME and APP_SECURITY_PASSWORD, or neither; only one is set");
		}
		if (production && !enabled()) {
			throw new IllegalStateException(
					"The prod profile requires a login: set APP_SECURITY_USERNAME and APP_SECURITY_PASSWORD");
		}
	}

	@Override
	public String toString() {
		return "AppSecurityProperties[username=" + this.username + ", password=<hidden>]";
	}

}

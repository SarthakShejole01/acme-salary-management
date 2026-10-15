package com.acme.salary.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * One shared HTTP basic-auth login in front of the whole application: the Angular app, the API and the CSV export
 * (ADR 005). Only the health check stays open, for the hosting platform. Without configured credentials everything
 * is open, which is only allowed outside the {@code prod} profile.
 *
 * <p>
 * Stateless, with CSRF protection off: writes need a JSON body, which another site cannot send from a browser
 * without a CORS preflight, and this API never answers one.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

	static final String REALM = "ACME Salary Management";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AppSecurityProperties properties,
			Environment environment) throws Exception {
		properties.validate(environment.acceptsProfiles(Profiles.of("prod")));
		http.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		if (properties.enabled()) {
			http.authorizeHttpRequests((requests) -> requests.requestMatchers("/actuator/health", "/actuator/health/**")
				.permitAll()
				.anyRequest()
				.authenticated()).httpBasic((basic) -> basic.realmName(REALM));
		}
		else {
			logger.warn("No login configured (APP_SECURITY_USERNAME / APP_SECURITY_PASSWORD): the application is open");
			http.authorizeHttpRequests((requests) -> requests.anyRequest().permitAll());
		}
		return http.build();
	}

	@Bean
	UserDetailsService userDetailsService(AppSecurityProperties properties) {
		if (!properties.enabled()) {
			return new InMemoryUserDetailsManager();
		}
		return new InMemoryUserDetailsManager(
				User.withUsername(properties.username()).password(properties.password()).roles("HR").build());
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new ConstantTimePasswordEncoder();
	}

	/**
	 * The password comes from an environment variable, so there is nothing to hash at rest; what matters is that
	 * comparing it takes the same time however many leading characters match. Digesting both sides first makes the
	 * inputs equal in length, and {@link MessageDigest#isEqual} compares them in constant time. (BCrypt would add
	 * about 100 ms of CPU to every request, because the browser sends the login with each one.)
	 */
	static final class ConstantTimePasswordEncoder implements PasswordEncoder {

		@Override
		public String encode(CharSequence rawPassword) {
			return rawPassword.toString();
		}

		@Override
		public boolean matches(CharSequence rawPassword, String storedPassword) {
			return MessageDigest.isEqual(sha256(rawPassword.toString()), sha256(storedPassword));
		}

		private static byte[] sha256(String value) {
			try {
				return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			}
			catch (NoSuchAlgorithmException ex) {
				throw new IllegalStateException("SHA-256 is always available", ex);
			}
		}

	}

}

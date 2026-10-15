package com.acme.salary.common;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The single source of "now" for the application, so tests can replace it with a fixed clock. */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}

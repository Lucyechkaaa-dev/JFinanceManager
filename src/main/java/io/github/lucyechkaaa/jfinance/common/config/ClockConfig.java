package io.github.lucyechkaaa.jfinance.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes a {@link Clock} so time-dependent logic (lockouts, timestamps) is testable. */
@Configuration
public class ClockConfig {

	/** UTC system clock. */
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}

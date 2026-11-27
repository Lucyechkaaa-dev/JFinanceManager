package io.github.lucyechkaaa.jfinance.common.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes a {@link Clock} so time-dependent logic (lockouts, timestamps) is testable. */
@Configuration
public class ClockConfig {

	/** SQLite stores timestamps with millisecond precision. */
	private static final Duration STORAGE_PRECISION = Duration.ofMillis(1);

	/**
	 * UTC clock that ticks in whole milliseconds, so every timestamp the application creates is
	 * read back from the database unchanged.
	 */
	@Bean
	public Clock clock() {
		return Clock.tick(Clock.systemUTC(), STORAGE_PRECISION);
	}
}

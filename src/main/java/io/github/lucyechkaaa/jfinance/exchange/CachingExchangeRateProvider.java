package io.github.lucyechkaaa.jfinance.exchange;

import io.github.lucyechkaaa.jfinance.common.exception.ExternalServiceException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator: caches the rates of any other {@link ExchangeRateProvider} for a fixed time and,
 * when a refresh fails, keeps serving the last successful rates.
 */
public final class CachingExchangeRateProvider implements ExchangeRateProvider {

	private static final Logger log = LoggerFactory.getLogger(CachingExchangeRateProvider.class);

	private final ExchangeRateProvider delegate;
	private final Duration ttl;
	private final Clock clock;

	private ExchangeRates cached;
	private Instant cachedAt;

	/**
	 * Wraps {@code delegate}.
	 *
	 * @throws IllegalArgumentException if {@code ttl} is not positive
	 */
	public CachingExchangeRateProvider(ExchangeRateProvider delegate, Duration ttl, Clock clock) {
		this.delegate = Objects.requireNonNull(delegate, "delegate");
		this.clock = Objects.requireNonNull(clock, "clock");
		if (ttl.isZero() || ttl.isNegative()) {
			throw new IllegalArgumentException("TTL must be positive: " + ttl);
		}
		this.ttl = ttl;
	}

	@Override
	public synchronized ExchangeRates currentRates() {
		Instant now = clock.instant();
		if (cached != null && now.isBefore(cachedAt.plus(ttl))) {
			return cached;
		}
		try {
			cached = delegate.currentRates();
			cachedAt = now;
			return cached;
		} catch (ExternalServiceException ex) {
			if (cached == null) {
				throw ex;
			}
			log.warn("Rates refresh failed, using rates from {}: {}", cached.date(), ex.getMessage());
			return cached;
		}
	}
}

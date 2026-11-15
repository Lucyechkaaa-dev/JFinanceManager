package io.github.lucyechkaaa.jfinance.exchange;

import java.time.Clock;
import java.time.Duration;

/** Decorator that caches rates of another provider and falls back to stale rates on failure. */
public class CachingExchangeRateProvider implements ExchangeRateProvider {

	public CachingExchangeRateProvider(ExchangeRateProvider delegate, Duration ttl, Clock clock) {
		// not implemented yet
	}

	@Override
	public ExchangeRates currentRates() {
		throw new UnsupportedOperationException("not implemented yet");
	}
}

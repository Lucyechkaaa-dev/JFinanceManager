package io.github.lucyechkaaa.jfinance.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lucyechkaaa.jfinance.common.exception.ExternalServiceException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Currency;
import java.util.Deque;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** NFR-3: at most one NBU request per TTL, stale rates when NBU is down. */
class CachingExchangeRateProviderTest {

	private static final Duration TTL = Duration.ofMinutes(60);
	private static final Currency UAH = Currency.getInstance("UAH");
	private static final Currency USD = Currency.getInstance("USD");

	private MutableClock clock;
	private ScriptedProvider delegate;
	private CachingExchangeRateProvider provider;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(Instant.parse("2026-11-27T09:00:00Z"));
		delegate = new ScriptedProvider();
		provider = new CachingExchangeRateProvider(delegate, TTL, clock);
	}

	@Test
	void firstCallLoadsFromDelegate() {
		ExchangeRates rates = rates("41.50");
		delegate.willReturn(rates);

		assertThat(provider.currentRates()).isSameAs(rates);
		assertThat(delegate.calls).isEqualTo(1);
	}

	@Test
	void ratesAreReusedUntilTtlExpires() {
		delegate.willReturn(rates("41.50"));
		ExchangeRates first = provider.currentRates();

		clock.advance(TTL.minusNanos(1));

		assertThat(provider.currentRates()).isSameAs(first);
		assertThat(delegate.calls).isEqualTo(1);
	}

	@Test
	void ratesAreRefreshedExactlyWhenTtlExpires() {
		ExchangeRates fresh = rates("42.00");
		delegate.willReturn(rates("41.50"));
		delegate.willReturn(fresh);
		provider.currentRates();

		clock.advance(TTL);

		assertThat(provider.currentRates()).isSameAs(fresh);
		assertThat(delegate.calls).isEqualTo(2);
	}

	@Test
	void staleRatesAreUsedWhenRefreshFails() {
		ExchangeRates stale = rates("41.50");
		delegate.willReturn(stale);
		delegate.willFail();
		provider.currentRates();
		clock.advance(TTL.multipliedBy(3));

		assertThat(provider.currentRates()).isSameAs(stale);
	}

	@Test
	void failureWithoutCachedRatesIsPropagated() {
		delegate.willFail();

		assertThatThrownBy(provider::currentRates).isInstanceOf(ExternalServiceException.class);
	}

	@Test
	void failureIsNotCachedSoNextCallRetries() {
		ExchangeRates recovered = rates("41.50");
		delegate.willFail();
		delegate.willReturn(recovered);

		assertThatThrownBy(provider::currentRates).isInstanceOf(ExternalServiceException.class);
		assertThat(provider.currentRates()).isSameAs(recovered);
		assertThat(delegate.calls).isEqualTo(2);
	}

	@Test
	void rejectsInvalidConfiguration() {
		assertThatThrownBy(() -> new CachingExchangeRateProvider(null, TTL, clock))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new CachingExchangeRateProvider(delegate, Duration.ZERO, clock))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new CachingExchangeRateProvider(delegate, TTL.negated(), clock))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static ExchangeRates rates(String usd) {
		return new ExchangeRates(UAH, Map.of(USD, new BigDecimal(usd)), LocalDate.of(2026, 11, 27));
	}

	/** Returns or fails according to a queued script and counts calls. */
	private static final class ScriptedProvider implements ExchangeRateProvider {

		private final Deque<ExchangeRates> script = new ArrayDeque<>();
		private int calls;

		void willReturn(ExchangeRates rates) {
			script.add(rates);
		}

		void willFail() {
			script.add(FAILURE);
		}

		@Override
		public ExchangeRates currentRates() {
			calls++;
			ExchangeRates next = script.isEmpty() ? FAILURE : script.poll();
			if (next == FAILURE) {
				throw new ExternalServiceException("NBU down", new IllegalStateException());
			}
			return next;
		}

		private static final ExchangeRates FAILURE =
				new ExchangeRates(UAH, Map.of(), LocalDate.MIN);
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}
	}
}

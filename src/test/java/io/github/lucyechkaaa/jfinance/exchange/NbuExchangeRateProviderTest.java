package io.github.lucyechkaaa.jfinance.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.lucyechkaaa.jfinance.common.exception.ExternalServiceException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NbuExchangeRateProviderTest {

	private static final String URL = "https://nbu.test/rates";
	private static final Currency UAH = Currency.getInstance("UAH");
	private static final Currency USD = Currency.getInstance("USD");
	private static final Currency EUR = Currency.getInstance("EUR");

	/** Trimmed real NBU response. XAU (gold) and CAD (not supported) must be ignored. */
	private static final String BODY =
			"""
			[
			 {"r030":840,"txt":"Долар США","rate":45.1064,"cc":"USD","exchangedate":"27.11.2026"},
			 {"r030":978,"txt":"Євро","rate":49.4942,"cc":"EUR","exchangedate":"27.11.2026"},
			 {"r030":124,"txt":"Канадський долар","rate":32.1,"cc":"CAD","exchangedate":"27.11.2026"},
			 {"r030":959,"txt":"Золото","rate":170000.5,"cc":"XAU","exchangedate":"27.11.2026"}
			]
			""";

	private MockRestServiceServer server;
	private MutableClock clock;
	private NbuExchangeRateProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		clock = new MutableClock(Instant.parse("2026-11-27T10:00:00Z"));
		provider = new NbuExchangeRateProvider(
				builder.build(),
				new NbuProperties(URI.create(URL), Duration.ofHours(1)),
				new CurrencyProperties(UAH, Set.of(UAH, USD, EUR)),
				clock);
	}

	@Test
	void parsesSupportedCurrenciesWithExactDecimals() {
		server.expect(requestTo(URL)).andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

		ExchangeRates rates = provider.currentRates();

		assertThat(rates.date()).isEqualTo(LocalDate.of(2026, 11, 27));
		assertThat(rates.rates()).containsOnlyKeys(USD, EUR);
		assertThat(rates.toBase(USD)).isEqualByComparingTo("45.1064");
		assertThat(rates.toBase(UAH)).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void cachesWithinTtlAndRefreshesAfterIt() {
		server.expect(ExpectedCount.twice(), requestTo(URL))
				.andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

		provider.currentRates();
		clock.advance(Duration.ofMinutes(59));
		provider.currentRates(); // cached, no HTTP call
		clock.advance(Duration.ofMinutes(2));
		provider.currentRates(); // expired, second HTTP call

		server.verify();
	}

	@Test
	void fallsBackToStaleRatesWhenNbuFails() {
		server.expect(requestTo(URL)).andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
		server.expect(requestTo(URL)).andRespond(withServerError());

		ExchangeRates first = provider.currentRates();
		clock.advance(Duration.ofHours(2));

		assertThat(provider.currentRates()).isSameAs(first);
	}

	@Test
	void failsWhenNbuUnavailableAndNothingCached() {
		server.expect(requestTo(URL)).andRespond(withServerError());

		assertThatThrownBy(provider::currentRates).isInstanceOf(ExternalServiceException.class);
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
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}
	}
}

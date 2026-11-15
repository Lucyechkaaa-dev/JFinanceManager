package io.github.lucyechkaaa.jfinance.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.lucyechkaaa.jfinance.common.exception.ExternalServiceException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Adapter from the NBU JSON API to {@link ExchangeRates}. */
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
	private NbuExchangeRateProvider provider;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		provider = new NbuExchangeRateProvider(
				builder.build(),
				new NbuProperties(URI.create(URL), Duration.ofHours(1)),
				new CurrencyProperties(UAH, Set.of(UAH, USD, EUR)));
	}

	@Test
	void parsesSupportedCurrenciesWithExactDecimals() {
		respondWith(BODY);

		ExchangeRates rates = provider.currentRates();

		assertThat(rates.date()).isEqualTo(LocalDate.of(2026, 11, 27));
		assertThat(rates.rates()).containsOnlyKeys(USD, EUR);
		assertThat(rates.toBase(USD)).isEqualByComparingTo("45.1064");
		assertThat(rates.toBase(UAH)).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void everyCallIsOneRequestBecauseCachingIsNotTheAdaptersJob() {
		server.expect(ExpectedCount.twice(), requestTo(URL))
				.andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

		provider.currentRates();
		provider.currentRates();

		server.verify();
	}

	@Test
	void serverErrorBecomesExternalServiceException() {
		server.expect(requestTo(URL)).andRespond(withServerError());

		assertThatThrownBy(provider::currentRates).isInstanceOf(ExternalServiceException.class);
	}

	@Test
	void emptyResponseIsRejected() {
		respondWith("[]");

		assertThatThrownBy(provider::currentRates).isInstanceOf(ExternalServiceException.class);
	}

	@Test
	void malformedDateIsRejected() {
		respondWith("[{\"rate\":45.1,\"cc\":\"USD\",\"exchangedate\":\"2026-11-27\"}]");

		assertThatThrownBy(provider::currentRates).isInstanceOf(ExternalServiceException.class);
	}

	private void respondWith(String body) {
		server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
	}
}

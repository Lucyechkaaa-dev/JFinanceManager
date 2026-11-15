package io.github.lucyechkaaa.jfinance.exchange;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wires the exchange-rate source: the NBU adapter wrapped in the caching decorator. Business
 * code only sees {@link ExchangeRateProvider}.
 */
@Configuration
public class NbuClientConfig {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	/** Rates from NBU, cached for {@link NbuProperties#cacheTtl()}. */
	@Bean
	public ExchangeRateProvider exchangeRateProvider(
			RestClient.Builder restClientBuilder,
			NbuProperties nbuProperties,
			CurrencyProperties currencyProperties,
			Clock clock) {
		ExchangeRateProvider nbu = new NbuExchangeRateProvider(
				nbuRestClient(restClientBuilder), nbuProperties, currencyProperties);
		return new CachingExchangeRateProvider(nbu, nbuProperties.cacheTtl(), clock);
	}

	private static RestClient nbuRestClient(RestClient.Builder builder) {
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
		requestFactory.setReadTimeout(TIMEOUT);
		return builder.requestFactory(requestFactory).build();
	}
}

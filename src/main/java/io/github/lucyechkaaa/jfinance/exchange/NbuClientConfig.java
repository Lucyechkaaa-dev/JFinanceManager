package io.github.lucyechkaaa.jfinance.exchange;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** HTTP client for the NBU API with explicit timeouts, so a slow NBU cannot hang requests. */
@Configuration
public class NbuClientConfig {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);

	/** REST client used by {@link NbuExchangeRateProvider}. */
	@Bean
	public RestClient nbuRestClient(RestClient.Builder builder) {
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
		requestFactory.setReadTimeout(TIMEOUT);
		return builder.requestFactory(requestFactory).build();
	}
}

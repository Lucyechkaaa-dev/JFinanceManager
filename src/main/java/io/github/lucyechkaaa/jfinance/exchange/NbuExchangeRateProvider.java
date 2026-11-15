package io.github.lucyechkaaa.jfinance.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.lucyechkaaa.jfinance.common.exception.ExternalServiceException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter: turns the NBU HTTP API (JSON array, {@code dd.MM.yyyy} dates, non-ISO codes such as
 * metals) into the {@link ExchangeRateProvider} interface used by the rest of the system. Every
 * call performs one HTTP request; caching is added by {@link CachingExchangeRateProvider}.
 */
public final class NbuExchangeRateProvider implements ExchangeRateProvider {

	private static final DateTimeFormatter NBU_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private final RestClient restClient;
	private final NbuProperties nbuProperties;
	private final CurrencyProperties currencyProperties;

	public NbuExchangeRateProvider(
			RestClient nbuRestClient,
			NbuProperties nbuProperties,
			CurrencyProperties currencyProperties) {
		this.restClient = nbuRestClient;
		this.nbuProperties = nbuProperties;
		this.currencyProperties = currencyProperties;
	}

	/**
	 * Fetches today's official rates for the supported currencies.
	 *
	 * @throws ExternalServiceException if NBU is unreachable or answers with unexpected data
	 */
	@Override
	public ExchangeRates currentRates() {
		try {
			return toExchangeRates(fetch());
		} catch (RestClientException | IllegalArgumentException | DateTimeException ex) {
			throw new ExternalServiceException("NBU exchange rates are unavailable", ex);
		}
	}

	private List<NbuRate> fetch() {
		List<NbuRate> body = restClient.get()
				.uri(nbuProperties.url())
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		if (body == null || body.isEmpty()) {
			throw new IllegalArgumentException("NBU returned no rates");
		}
		return body;
	}

	private ExchangeRates toExchangeRates(List<NbuRate> body) {
		Map<Currency, BigDecimal> rates = new HashMap<>();
		for (NbuRate rate : body) {
			toSupportedCurrency(rate.cc())
					.filter(currency -> rate.rate() != null)
					.ifPresent(currency -> rates.put(currency, rate.rate()));
		}
		LocalDate date = LocalDate.parse(body.getFirst().exchangedate(), NBU_DATE);
		return new ExchangeRates(currencyProperties.base(), rates, date);
	}

	private Optional<Currency> toSupportedCurrency(String code) {
		return Optional.ofNullable(code)
				.flatMap(NbuExchangeRateProvider::toIsoCurrency)
				.filter(currencyProperties::isSupported);
	}

	private static Optional<Currency> toIsoCurrency(String code) {
		try {
			return Optional.of(Currency.getInstance(code));
		} catch (IllegalArgumentException ex) {
			return Optional.empty(); // NBU also lists non-ISO codes such as precious metals
		}
	}

	/** One element of the NBU JSON array. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record NbuRate(String cc, BigDecimal rate, String exchangedate) {
	}
}

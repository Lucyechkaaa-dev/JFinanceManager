package io.github.lucyechkaaa.jfinance.exchange;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.lucyechkaaa.jfinance.common.exception.ExternalServiceException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Fetches official rates from the NBU API and caches them for {@link NbuProperties#cacheTtl()}.
 * If a refresh fails, the last successful snapshot is reused (with a warning) so that a short
 * NBU outage does not stop exchanges.
 */
@Component
public class NbuExchangeRateProvider implements ExchangeRateProvider {

	private static final Logger log = LoggerFactory.getLogger(NbuExchangeRateProvider.class);
	private static final DateTimeFormatter NBU_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

	private final RestClient restClient;
	private final NbuProperties nbuProperties;
	private final CurrencyProperties currencyProperties;
	private final Clock clock;

	private ExchangeRates cached;
	private Instant cachedAt;

	public NbuExchangeRateProvider(
			RestClient nbuRestClient,
			NbuProperties nbuProperties,
			CurrencyProperties currencyProperties,
			Clock clock) {
		this.restClient = nbuRestClient;
		this.nbuProperties = nbuProperties;
		this.currencyProperties = currencyProperties;
		this.clock = clock;
	}

	@Override
	public synchronized ExchangeRates currentRates() {
		Instant now = clock.instant();
		if (cached != null && now.isBefore(cachedAt.plus(nbuProperties.cacheTtl()))) {
			return cached;
		}
		try {
			cached = fetch();
			cachedAt = now;
			log.info("NBU rates refreshed for {}: {}", cached.date(), cached.rates());
			return cached;
		} catch (RestClientException | IllegalArgumentException | DateTimeException ex) {
			if (cached != null) {
				log.warn("NBU refresh failed, using rates from {}: {}",
						cached.date(), ex.getMessage());
				return cached;
			}
			throw new ExternalServiceException("NBU exchange rates are unavailable", ex);
		}
	}

	private ExchangeRates fetch() {
		List<NbuRate> body = restClient.get()
				.uri(nbuProperties.url())
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		if (body == null || body.isEmpty()) {
			throw new IllegalArgumentException("NBU returned no rates");
		}
		Map<Currency, BigDecimal> rates = new HashMap<>();
		for (NbuRate rate : body) {
			Currency currency = toCurrency(rate.cc());
			boolean usable = currency != null && rate.rate() != null;
			if (usable && currencyProperties.isSupported(currency)) {
				rates.put(currency, rate.rate());
			}
		}
		LocalDate date = LocalDate.parse(body.getFirst().exchangedate(), NBU_DATE);
		return new ExchangeRates(currencyProperties.base(), rates, date);
	}

	private static Currency toCurrency(String code) {
		try {
			return code == null ? null : Currency.getInstance(code);
		} catch (IllegalArgumentException ex) {
			return null; // NBU also lists non-ISO codes such as precious metals
		}
	}

	/** One element of the NBU JSON array. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record NbuRate(String cc, BigDecimal rate, String exchangedate) {
	}
}

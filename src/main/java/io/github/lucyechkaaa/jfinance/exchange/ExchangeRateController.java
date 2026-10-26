package io.github.lucyechkaaa.jfinance.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes current official rates. */
@RestController
@RequestMapping("/api/rates")
public class ExchangeRateController {

	private final ExchangeRateProvider rateProvider;

	public ExchangeRateController(ExchangeRateProvider rateProvider) {
		this.rateProvider = rateProvider;
	}

	/** Current NBU rates for the supported currencies. */
	@GetMapping
	public RatesResponse current() {
		ExchangeRates rates = rateProvider.currentRates();
		Map<String, BigDecimal> byCode = new TreeMap<>();
		rates.rates().forEach((currency, rate) -> byCode.put(currency.getCurrencyCode(), rate));
		return new RatesResponse(rates.base().getCurrencyCode(), rates.date(), byCode);
	}

	/** Rates as units of {@code base} per one unit of each currency. */
	public record RatesResponse(String base, LocalDate date, Map<String, BigDecimal> rates) {
	}
}

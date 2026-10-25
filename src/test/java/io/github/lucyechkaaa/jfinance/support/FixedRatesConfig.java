package io.github.lucyechkaaa.jfinance.support;

import io.github.lucyechkaaa.jfinance.exchange.ExchangeRateProvider;
import io.github.lucyechkaaa.jfinance.exchange.ExchangeRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Deterministic rates for integration tests: 1 USD = 40 UAH, 1 EUR = 50 UAH. */
@TestConfiguration
public class FixedRatesConfig {

	@Bean
	@Primary
	ExchangeRateProvider fixedRateProvider() {
		return () -> new ExchangeRates(
				Currency.getInstance("UAH"),
				Map.of(Currency.getInstance("USD"), new BigDecimal("40"),
						Currency.getInstance("EUR"), new BigDecimal("50"),
						Currency.getInstance("GBP"), new BigDecimal("60"),
						Currency.getInstance("PLN"), new BigDecimal("10")),
				LocalDate.of(2026, 11, 26));
	}
}

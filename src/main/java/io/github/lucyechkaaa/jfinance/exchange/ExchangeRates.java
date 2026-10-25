package io.github.lucyechkaaa.jfinance.exchange;

import io.github.lucyechkaaa.jfinance.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Map;
import java.util.Objects;

/**
 * Snapshot of official rates: how many units of {@code base} one unit of each currency costs.
 *
 * @param base  quote currency (UAH for NBU)
 * @param rates units of base per one unit of the key currency; base itself is implicit (1)
 * @param date  official date of the rates
 */
public record ExchangeRates(Currency base, Map<Currency, BigDecimal> rates, LocalDate date) {

	private static final MathContext RATE_PRECISION = MathContext.DECIMAL64;

	public ExchangeRates {
		Objects.requireNonNull(base);
		Objects.requireNonNull(date);
		rates = Map.copyOf(rates);
	}

	/** Units of base currency per one unit of {@code currency}. */
	public BigDecimal toBase(Currency currency) {
		if (currency.equals(base)) {
			return BigDecimal.ONE;
		}
		BigDecimal rate = rates.get(currency);
		if (rate == null) {
			throw new BusinessRuleException("No official rate for " + currency);
		}
		return rate;
	}

	/** Cross rate: units of {@code to} per one unit of {@code from}, via the base currency. */
	public BigDecimal crossRate(Currency from, Currency to) {
		return toBase(from).divide(toBase(to), RATE_PRECISION).stripTrailingZeros();
	}
}

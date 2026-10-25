package io.github.lucyechkaaa.jfinance.exchange;

import io.github.lucyechkaaa.jfinance.common.exception.BusinessRuleException;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import org.springframework.stereotype.Service;

/** Converts money using official NBU cross rates. */
@Service
public class CurrencyConverter {

	private final ExchangeRateProvider rateProvider;

	public CurrencyConverter(ExchangeRateProvider rateProvider) {
		this.rateProvider = rateProvider;
	}

	/**
	 * Converts {@code amount} to {@code target}. The result is rounded <b>down</b> to the target
	 * currency's minor unit, so the system can never credit more value than it debited.
	 *
	 * @throws BusinessRuleException if the converted amount rounds to zero
	 */
	public Conversion convert(Money amount, Currency target) {
		if (amount.currency().equals(target)) {
			return new Conversion(amount, amount, null);
		}
		ExchangeRates rates = rateProvider.currentRates();
		BigDecimal converted = amount.amount()
				.multiply(rates.toBase(amount.currency()))
				.divide(rates.toBase(target), target.getDefaultFractionDigits(), RoundingMode.DOWN);
		Money result = new Money(converted, target);
		if (!result.isPositive()) {
			throw new BusinessRuleException(
					"Amount %s is too small to convert to %s".formatted(amount, target));
		}
		return new Conversion(amount, result, rates.crossRate(amount.currency(), target));
	}
}

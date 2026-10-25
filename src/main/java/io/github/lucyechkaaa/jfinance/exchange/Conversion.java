package io.github.lucyechkaaa.jfinance.exchange;

import io.github.lucyechkaaa.jfinance.common.money.Money;
import java.math.BigDecimal;

/**
 * Result of converting money between currencies.
 *
 * @param source original amount
 * @param result converted amount (rounded down, so conversion never creates money)
 * @param rate   applied cross rate, units of target per one unit of source; {@code null} when
 *               no conversion was needed
 */
public record Conversion(Money source, Money result, BigDecimal rate) {

	public boolean isCrossCurrency() {
		return rate != null;
	}
}

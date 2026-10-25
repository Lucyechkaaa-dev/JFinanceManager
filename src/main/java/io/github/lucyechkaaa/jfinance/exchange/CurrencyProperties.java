package io.github.lucyechkaaa.jfinance.exchange;

import java.util.Currency;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Currencies the system works with.
 *
 * @param base      national currency; NBU quotes every rate against it
 * @param supported currencies in which accounts can be opened (must include {@code base})
 */
@ConfigurationProperties(prefix = "jfinance.currencies")
public record CurrencyProperties(Currency base, Set<Currency> supported) {

	public CurrencyProperties {
		supported = Set.copyOf(supported);
		if (!supported.contains(base)) {
			throw new IllegalArgumentException("Supported currencies must include base " + base);
		}
	}

	/** Whether accounts may be opened in {@code currency}. */
	public boolean isSupported(Currency currency) {
		return supported.contains(currency);
	}
}

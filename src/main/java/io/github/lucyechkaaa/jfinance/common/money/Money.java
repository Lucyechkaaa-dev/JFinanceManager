package io.github.lucyechkaaa.jfinance.common.money;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable monetary amount in a specific currency.
 *
 * <p>Internally the amount is always normalised to the currency's default fraction digits
 * (e.g. 2 for UAH/USD), so {@code Money} values can be converted losslessly to minor units
 * (kopiykas, cents) for storage.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

	/** Largest number of integer digits accepted in API requests. */
	public static final int MAX_INTEGER_DIGITS = 15;
	/** Fraction digits accepted in API requests (kopiykas, cents). */
	public static final int MAX_FRACTION_DIGITS = 2;

	public Money {
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
		amount = amount.setScale(scaleOf(currency), RoundingMode.UNNECESSARY);
	}

	/**
	 * Creates money from a decimal amount, rejecting values with more fraction digits than the
	 * currency allows.
	 *
	 * @throws IllegalArgumentException if the amount has too many fraction digits
	 */
	public static Money of(BigDecimal amount, Currency currency) {
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
		if (amount.stripTrailingZeros().scale() > scaleOf(currency)) {
			throw new IllegalArgumentException(
					"Amount %s has more than %d fraction digits allowed for %s"
							.formatted(amount.toPlainString(), scaleOf(currency), currency));
		}
		return new Money(amount, currency);
	}

	/** Convenience factory, e.g. {@code Money.of("10.50", "UAH")}. */
	public static Money of(String amount, String currencyCode) {
		return of(new BigDecimal(amount), Currency.getInstance(currencyCode));
	}

	/** Creates money from minor units, e.g. 1050 kopiykas = 10.50 UAH. */
	public static Money ofMinor(long minorUnits, Currency currency) {
		return new Money(BigDecimal.valueOf(minorUnits, scaleOf(currency)), currency);
	}

	/** Zero amount in the given currency. */
	public static Money zero(Currency currency) {
		return ofMinor(0, currency);
	}

	/** Amount in minor units (kopiykas, cents), as stored in the database. */
	public long toMinorUnits() {
		return amount.movePointRight(scaleOf(currency)).longValueExact();
	}

	/** Sum of two amounts in the same currency. */
	public Money plus(Money other) {
		requireSameCurrency(other);
		return new Money(amount.add(other.amount), currency);
	}

	/** Difference of two amounts in the same currency; may be negative. */
	public Money minus(Money other) {
		requireSameCurrency(other);
		return new Money(amount.subtract(other.amount), currency);
	}

	/** Whether the amount is greater than zero. */
	@JsonIgnore
	public boolean isPositive() {
		return amount.signum() > 0;
	}

	/** Whether the amount is less than zero. */
	@JsonIgnore
	public boolean isNegative() {
		return amount.signum() < 0;
	}

	/** Whether this amount is smaller than {@code other} (same currency required). */
	public boolean isLessThan(Money other) {
		return compareTo(other) < 0;
	}

	@Override
	public int compareTo(Money other) {
		requireSameCurrency(other);
		return amount.compareTo(other.amount);
	}

	@Override
	public String toString() {
		return amount.toPlainString() + " " + currency.getCurrencyCode();
	}

	private void requireSameCurrency(Money other) {
		if (!currency.equals(other.currency)) {
			throw new IllegalArgumentException(
					"Currency mismatch: %s vs %s".formatted(currency, other.currency));
		}
	}

	private static int scaleOf(Currency currency) {
		return Math.max(currency.getDefaultFractionDigits(), 0);
	}
}

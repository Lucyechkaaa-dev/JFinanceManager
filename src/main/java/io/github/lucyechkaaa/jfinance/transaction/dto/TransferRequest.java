package io.github.lucyechkaaa.jfinance.transaction.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * Money movement request.
 *
 * @param fromCurrency currency of the sender's account to debit
 * @param toUsername   recipient; may be the sender themself for a currency exchange
 * @param toCurrency   currency of the recipient's account; defaults to {@code fromCurrency}
 * @param amount       amount in {@code fromCurrency}
 */
public record TransferRequest(
		@NotNull Currency fromCurrency,
		@NotBlank String toUsername,
		Currency toCurrency,
		@NotNull @Positive @Digits(integer = 15, fraction = 2) BigDecimal amount) {

	/** Target currency, defaulting to the source currency. */
	public Currency effectiveToCurrency() {
		return toCurrency != null ? toCurrency : fromCurrency;
	}
}

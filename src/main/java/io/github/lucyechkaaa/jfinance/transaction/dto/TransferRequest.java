package io.github.lucyechkaaa.jfinance.transaction.dto;

import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.transaction.FinancialTransaction;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * Money movement request.
 *
 * @param fromCurrency currency of the sender's account to debit
 * @param toUsername   recipient; may be the sender themself for a currency exchange
 * @param toCurrency   currency of the recipient's account; defaults to {@code fromCurrency}
 * @param amount       amount in {@code fromCurrency}
 * @param description  optional free-text note shown in the history
 */
public record TransferRequest(
		@NotNull Currency fromCurrency,
		@NotBlank String toUsername,
		Currency toCurrency,
		@NotNull @Positive
		@Digits(integer = Money.MAX_INTEGER_DIGITS, fraction = Money.MAX_FRACTION_DIGITS)
		BigDecimal amount,
		@Size(max = FinancialTransaction.MAX_DESCRIPTION_LENGTH) String description) {

	/** Target currency, defaulting to the source currency. */
	public Currency effectiveToCurrency() {
		return toCurrency != null ? toCurrency : fromCurrency;
	}
}

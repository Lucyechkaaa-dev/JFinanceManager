package io.github.lucyechkaaa.jfinance.transaction;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Everything needed to move money: who sends, from which currency account, to whom, into which
 * currency account, how much and why.
 *
 * @param senderUsername    authenticated sender
 * @param fromCurrency      currency of the sender's account to debit
 * @param recipientUsername recipient; equal to the sender for a currency exchange
 * @param toCurrency        currency of the recipient's account to credit
 * @param amount            amount in {@code fromCurrency}
 * @param description       optional note, {@code null} when absent
 */
public record TransferCommand(
		String senderUsername,
		Currency fromCurrency,
		String recipientUsername,
		Currency toCurrency,
		BigDecimal amount,
		String description) {

	public TransferCommand {
		Objects.requireNonNull(senderUsername, "senderUsername");
		Objects.requireNonNull(fromCurrency, "fromCurrency");
		Objects.requireNonNull(recipientUsername, "recipientUsername");
		Objects.requireNonNull(toCurrency, "toCurrency");
		Objects.requireNonNull(amount, "amount");
	}
}

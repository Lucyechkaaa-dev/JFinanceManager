package io.github.lucyechkaaa.jfinance.transaction.dto;

import io.github.lucyechkaaa.jfinance.common.money.Money;

/**
 * Stored balance of an account next to the balance recalculated from the ledger.
 *
 * @param accountId     account
 * @param storedBalance balance column of the account
 * @param ledgerBalance credits minus debits from the transaction ledger
 * @param consistent    whether both values are equal
 */
public record BalanceReport(
		Long accountId, Money storedBalance, Money ledgerBalance, boolean consistent) {

	/** Builds a report and derives {@code consistent} from the two balances. */
	public static BalanceReport of(Long accountId, Money storedBalance, Money ledgerBalance) {
		return new BalanceReport(
				accountId, storedBalance, ledgerBalance, storedBalance.equals(ledgerBalance));
	}
}

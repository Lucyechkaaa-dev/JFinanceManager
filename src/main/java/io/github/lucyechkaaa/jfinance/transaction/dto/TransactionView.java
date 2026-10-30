package io.github.lucyechkaaa.jfinance.transaction.dto;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.transaction.FinancialTransaction;
import io.github.lucyechkaaa.jfinance.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read model of a ledger record. Built inside a transaction, so lazy associations are already
 * resolved when it reaches the web layer.
 */
public record TransactionView(
		Long id,
		TransactionType type,
		Long fromAccountId,
		String fromUsername,
		Money debit,
		Long toAccountId,
		String toUsername,
		Money credit,
		BigDecimal exchangeRate,
		Long reversalOfId,
		String initiatedBy,
		Instant createdAt) {

	/** Maps a ledger record; must be called inside a transaction (lazy associations). */
	public static TransactionView from(FinancialTransaction tx) {
		return new TransactionView(
				tx.getId(),
				tx.getType(),
				tx.getSourceAccount().map(Account::getId).orElse(null),
				tx.getSourceAccount().map(a -> a.getOwner().getUsername()).orElse(null),
				tx.getDebit().orElse(null),
				tx.getTargetAccount().map(Account::getId).orElse(null),
				tx.getTargetAccount().map(a -> a.getOwner().getUsername()).orElse(null),
				tx.getCredit().orElse(null),
				tx.getExchangeRate().orElse(null),
				tx.getReversalOf().map(FinancialTransaction::getId).orElse(null),
				tx.getInitiatedBy().getUsername(),
				tx.getCreatedAt());
	}
}

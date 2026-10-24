package io.github.lucyechkaaa.jfinance.transaction;

/** Kind of ledger record. */
public enum TransactionType {
	/** Admin adds money to an account. Has only a target side. */
	DEPOSIT,
	/** Money moves between accounts of different users. */
	TRANSFER,
	/** Money moves between two accounts of the same user in different currencies. */
	EXCHANGE,
	/** Compensating record that cancels a previous transaction (storno). */
	REVERSAL
}

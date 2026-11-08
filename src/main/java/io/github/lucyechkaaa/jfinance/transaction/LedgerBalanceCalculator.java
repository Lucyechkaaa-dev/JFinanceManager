package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import org.springframework.stereotype.Service;

/** Calculates an account balance from the transaction ledger. */
@Service
public class LedgerBalanceCalculator {

	/** Balance of {@code account} computed as the sum of its credits minus its debits. */
	public Money calculate(Account account) {
		throw new UnsupportedOperationException("not implemented yet");
	}
}

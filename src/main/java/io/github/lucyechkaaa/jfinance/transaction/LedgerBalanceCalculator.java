package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Calculates an account balance from the transaction ledger. */
@Service
public class LedgerBalanceCalculator {

	private final TransactionRepository transactionRepository;

	public LedgerBalanceCalculator(TransactionRepository transactionRepository) {
		this.transactionRepository = transactionRepository;
	}

	/** Balance of {@code account} computed as the sum of its credits minus its debits. */
	@Transactional(readOnly = true)
	public Money calculate(Account account) {
		long credits = transactionRepository.sumCreditsMinor(account);
		long debits = transactionRepository.sumDebitsMinor(account);
		return Money.ofMinor(credits - debits, account.getCurrency());
	}
}

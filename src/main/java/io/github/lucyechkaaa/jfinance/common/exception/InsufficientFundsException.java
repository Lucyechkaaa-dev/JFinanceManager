package io.github.lucyechkaaa.jfinance.common.exception;

import io.github.lucyechkaaa.jfinance.common.money.Money;

/** Account balance is lower than the requested debit (HTTP 422). */
public class InsufficientFundsException extends BusinessRuleException {

	public InsufficientFundsException(Money balance, Money requested) {
		super("Insufficient funds: balance %s, requested %s".formatted(balance, requested));
	}
}

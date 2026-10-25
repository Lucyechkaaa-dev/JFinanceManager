package io.github.lucyechkaaa.jfinance.account.dto;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import java.time.Instant;

/** Public view of an account. */
public record AccountResponse(Long id, Money balance, Instant createdAt) {

	/** Maps the entity to its public view. */
	public static AccountResponse from(Account account) {
		return new AccountResponse(account.getId(), account.getBalance(), account.getCreatedAt());
	}
}

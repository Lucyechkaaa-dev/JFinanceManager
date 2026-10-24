package io.github.lucyechkaaa.jfinance.account;

import io.github.lucyechkaaa.jfinance.common.exception.InsufficientFundsException;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

/**
 * Single-currency balance owned by a user. A user has at most one account per currency.
 *
 * <p>Balance is stored in minor units and can only change through {@link #credit(Money)} and
 * {@link #debit(Money)}, which enforce currency match and non-negative balance.
 */
@Entity
@Table(name = "accounts")
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	// SQLite AUTOINCREMENT requires the exact column type INTEGER.
	@Column(columnDefinition = "INTEGER")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private User owner;

	@Column(nullable = false, length = 3, updatable = false)
	private Currency currency;

	@Column(name = "balance_minor", nullable = false)
	private long balanceMinor;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Version
	private long version;

	protected Account() {
		// for JPA
	}

	public Account(User owner, Currency currency, Instant createdAt) {
		this.owner = Objects.requireNonNull(owner);
		this.currency = Objects.requireNonNull(currency);
		this.createdAt = Objects.requireNonNull(createdAt);
	}

	/** Current balance in the account currency. */
	public Money getBalance() {
		return Money.ofMinor(balanceMinor, currency);
	}

	/**
	 * Adds money to the account.
	 *
	 * @throws IllegalArgumentException if the amount is not positive or in another currency
	 */
	public void credit(Money amount) {
		requireValidAmount(amount);
		balanceMinor = getBalance().plus(amount).toMinorUnits();
	}

	/**
	 * Takes money from the account.
	 *
	 * @throws InsufficientFundsException if the balance is lower than {@code amount}
	 * @throws IllegalArgumentException   if the amount is not positive or in another currency
	 */
	public void debit(Money amount) {
		requireValidAmount(amount);
		Money balance = getBalance();
		if (balance.isLessThan(amount)) {
			throw new InsufficientFundsException(balance, amount);
		}
		balanceMinor = balance.minus(amount).toMinorUnits();
	}

	private void requireValidAmount(Money amount) {
		Objects.requireNonNull(amount, "amount must not be null");
		if (!amount.currency().equals(currency)) {
			throw new IllegalArgumentException(
					"Account %d is in %s, got %s".formatted(id, currency, amount.currency()));
		}
		if (!amount.isPositive()) {
			throw new IllegalArgumentException("Amount must be positive: " + amount);
		}
	}

	public Long getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public Currency getCurrency() {
		return currency;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

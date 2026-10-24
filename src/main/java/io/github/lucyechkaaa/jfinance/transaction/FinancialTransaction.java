package io.github.lucyechkaaa.jfinance.transaction;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.common.persistence.BigDecimalStringConverter;
import io.github.lucyechkaaa.jfinance.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.Immutable;

/**
 * Immutable ledger record. Every balance change in the system is backed by exactly one row.
 *
 * <p>The record has an optional debit side (money leaves {@code sourceAccount}) and an optional
 * credit side (money enters {@code targetAccount}). For cross-currency operations the two
 * sides are in different currencies and {@code exchangeRate} records the applied rate.
 */
@Entity
@Immutable
@Table(name = "transactions")
public class FinancialTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	// SQLite AUTOINCREMENT requires the exact column type INTEGER.
	@Column(columnDefinition = "INTEGER")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private TransactionType type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_account_id")
	private Account sourceAccount;

	@Column(name = "debit_amount_minor")
	private Long debitAmountMinor;

	@Column(name = "debit_currency", length = 3)
	private Currency debitCurrency;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "target_account_id")
	private Account targetAccount;

	@Column(name = "credit_amount_minor")
	private Long creditAmountMinor;

	@Column(name = "credit_currency", length = 3)
	private Currency creditCurrency;

	@Convert(converter = BigDecimalStringConverter.class)
	@Column(name = "exchange_rate", length = 32)
	private BigDecimal exchangeRate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reversal_of_id")
	private FinancialTransaction reversalOf;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "initiated_by_id", nullable = false)
	private User initiatedBy;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected FinancialTransaction() {
		// for JPA
	}

	private FinancialTransaction(TransactionType type, User initiatedBy, Instant createdAt) {
		this.type = Objects.requireNonNull(type);
		this.initiatedBy = Objects.requireNonNull(initiatedBy);
		this.createdAt = Objects.requireNonNull(createdAt);
	}

	/** Creates a DEPOSIT record: money added to {@code target} by an administrator. */
	public static FinancialTransaction deposit(
			Account target, Money amount, User admin, Instant now) {
		FinancialTransaction tx = new FinancialTransaction(TransactionType.DEPOSIT, admin, now);
		tx.setCreditSide(target, amount);
		return tx;
	}

	/**
	 * Creates a TRANSFER or EXCHANGE record.
	 *
	 * @param exchangeRate rate applied when currencies differ, {@code null} otherwise
	 */
	public static FinancialTransaction movement(
			TransactionType type,
			Account source,
			Money debit,
			Account target,
			Money credit,
			BigDecimal exchangeRate,
			User initiator,
			Instant now) {
		if (type != TransactionType.TRANSFER && type != TransactionType.EXCHANGE) {
			throw new IllegalArgumentException("Movement must be TRANSFER or EXCHANGE: " + type);
		}
		FinancialTransaction tx = new FinancialTransaction(type, initiator, now);
		tx.setDebitSide(source, debit);
		tx.setCreditSide(target, credit);
		tx.exchangeRate = exchangeRate;
		return tx;
	}

	/** Creates the compensating record: debit and credit sides are swapped. */
	public static FinancialTransaction reversalOf(
			FinancialTransaction original, User initiator, Instant now) {
		if (original.type == TransactionType.REVERSAL) {
			throw new IllegalArgumentException("A reversal cannot be reversed");
		}
		FinancialTransaction tx =
				new FinancialTransaction(TransactionType.REVERSAL, initiator, now);
		original.getCredit().ifPresent(credit -> tx.setDebitSide(original.targetAccount, credit));
		original.getDebit().ifPresent(debit -> tx.setCreditSide(original.sourceAccount, debit));
		tx.exchangeRate = original.exchangeRate;
		tx.reversalOf = original;
		return tx;
	}

	private void setDebitSide(Account account, Money amount) {
		this.sourceAccount = Objects.requireNonNull(account);
		this.debitAmountMinor = amount.toMinorUnits();
		this.debitCurrency = amount.currency();
	}

	private void setCreditSide(Account account, Money amount) {
		this.targetAccount = Objects.requireNonNull(account);
		this.creditAmountMinor = amount.toMinorUnits();
		this.creditCurrency = amount.currency();
	}

	/** Amount taken from the source account, if this record has a debit side. */
	public Optional<Money> getDebit() {
		return debitAmountMinor == null
				? Optional.empty()
				: Optional.of(Money.ofMinor(debitAmountMinor, debitCurrency));
	}

	/** Amount added to the target account, if this record has a credit side. */
	public Optional<Money> getCredit() {
		return creditAmountMinor == null
				? Optional.empty()
				: Optional.of(Money.ofMinor(creditAmountMinor, creditCurrency));
	}

	public Long getId() {
		return id;
	}

	public TransactionType getType() {
		return type;
	}

	public Optional<Account> getSourceAccount() {
		return Optional.ofNullable(sourceAccount);
	}

	public Optional<Account> getTargetAccount() {
		return Optional.ofNullable(targetAccount);
	}

	public Optional<BigDecimal> getExchangeRate() {
		return Optional.ofNullable(exchangeRate);
	}

	public Optional<FinancialTransaction> getReversalOf() {
		return Optional.ofNullable(reversalOf);
	}

	public User getInitiatedBy() {
		return initiatedBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

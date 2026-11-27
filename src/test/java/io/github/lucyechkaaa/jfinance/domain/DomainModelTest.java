package io.github.lucyechkaaa.jfinance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.common.exception.InsufficientFundsException;
import io.github.lucyechkaaa.jfinance.common.money.Money;
import io.github.lucyechkaaa.jfinance.transaction.FinancialTransaction;
import io.github.lucyechkaaa.jfinance.transaction.TransactionType;
import io.github.lucyechkaaa.jfinance.user.Role;
import io.github.lucyechkaaa.jfinance.user.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import org.junit.jupiter.api.Test;

/** Invariants enforced by the entities themselves. */
class DomainModelTest {

	private static final Instant NOW = Instant.parse("2026-11-26T12:00:00Z");
	private static final Currency UAH = Currency.getInstance("UAH");

	private final User user = new User("alice", "hash", Role.USER, NOW);

	@Test
	void accountRejectsOverdraftWrongCurrencyAndNonPositiveAmounts() {
		Account account = new Account(user, UAH, NOW);
		account.credit(Money.of("10", "UAH"));

		assertThatThrownBy(() -> account.debit(Money.of("10.01", "UAH")))
				.isInstanceOf(InsufficientFundsException.class);
		assertThatThrownBy(() -> account.credit(Money.of("1", "USD")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> account.credit(Money.zero(UAH)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(account.getBalance()).isEqualTo(Money.of("10", "UAH"));
	}

	@Test
	void debitOfTheWholeBalanceLeavesExactlyZero() {
		Account account = new Account(user, UAH, NOW);
		account.credit(Money.of("10", "UAH"));

		account.debit(Money.of("10", "UAH"));

		assertThat(account.getBalance()).isEqualTo(Money.zero(UAH));
	}

	@Test
	void smallestAmountsAreAccepted() {
		Account account = new Account(user, UAH, NOW);

		account.credit(Money.of("0.01", "UAH"));
		account.debit(Money.of("0.01", "UAH"));

		assertThat(account.getBalance()).isEqualTo(Money.zero(UAH));
	}

	@Test
	void negativeAmountsAreRejected() {
		Account account = new Account(user, UAH, NOW);
		account.credit(Money.of("5", "UAH"));

		assertThatThrownBy(() -> account.credit(Money.of("-1", "UAH")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> account.debit(Money.of("-1", "UAH")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(account.getBalance()).isEqualTo(Money.of("5", "UAH"));
	}

	@Test
	void nullArgumentsAreRejected() {
		Account account = new Account(user, UAH, NOW);

		assertThatThrownBy(() -> account.credit(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> account.debit(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new Account(null, UAH, NOW))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new User(null, "hash", Role.USER, NOW))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void reversalOfReversalIsImpossible() {
		Account source = new Account(user, UAH, NOW);
		Account target = new Account(user, Currency.getInstance("USD"), NOW);
		FinancialTransaction transfer = FinancialTransaction.movement(TransactionType.EXCHANGE,
				source, Money.of("40", "UAH"), target, Money.of("1", "USD"), null, null, user, NOW);
		FinancialTransaction reversal = FinancialTransaction.reversalOf(transfer, user, NOW);

		assertThat(reversal.getDebit()).contains(Money.of("1", "USD"));
		assertThat(reversal.getCredit()).contains(Money.of("40", "UAH"));
		assertThatThrownBy(() -> FinancialTransaction.reversalOf(reversal, user, NOW))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void depositCannotBeCreatedAsMovement() {
		Account account = new Account(user, UAH, NOW);
		Money amount = Money.of("1", "UAH");

		assertThatThrownBy(() -> FinancialTransaction.movement(TransactionType.DEPOSIT,
				account, amount, account, amount, null, null, user, NOW))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void userIsLockedAfterMaxFailuresAndUnlocksAfterDuration() {
		Duration lock = Duration.ofMinutes(15);
		for (int i = 0; i < 4; i++) {
			assertThat(user.registerFailedLogin(5, lock, NOW)).isFalse();
		}
		assertThat(user.registerFailedLogin(5, lock, NOW)).isTrue();

		assertThat(user.isLockedAt(NOW.plus(Duration.ofMinutes(14)))).isTrue();
		assertThat(user.isLockedAt(NOW.plus(lock))).isFalse();
	}

	@Test
	void successfulLoginResetsFailures() {
		user.registerFailedLogin(5, Duration.ofMinutes(15), NOW);
		user.registerSuccessfulLogin();

		assertThat(user.getFailedLoginAttempts()).isZero();
		assertThat(user.getLockedUntil()).isNull();
	}

	@Test
	void depositHasOnlyCreditSide() {
		Account account = new Account(user, UAH, NOW);

		FinancialTransaction deposit =
				FinancialTransaction.deposit(account, Money.of("5", "UAH"), user, NOW);

		assertThat(deposit.getType()).isEqualTo(TransactionType.DEPOSIT);
		assertThat(deposit.getCredit()).contains(Money.of("5", "UAH"));
		assertThat(deposit.getDebit()).isEmpty();
		assertThat(deposit.getSourceAccount()).isEmpty();
		assertThat(deposit.getTargetAccount()).contains(account);
	}
}

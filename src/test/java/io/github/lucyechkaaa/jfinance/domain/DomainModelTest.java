package io.github.lucyechkaaa.jfinance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lucyechkaaa.jfinance.account.Account;
import io.github.lucyechkaaa.jfinance.common.exception.InsufficientFundsException;
import io.github.lucyechkaaa.jfinance.common.money.Money;
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
}
